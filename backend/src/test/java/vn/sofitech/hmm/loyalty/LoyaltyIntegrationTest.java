package vn.sofitech.hmm.loyalty;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import vn.sofitech.hmm.shared.ApiException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm tra tích hợp với PostgreSQL thật: sổ cái điểm (FR-33), giữ / trả / đổi điểm theo FIFO và luồng OTP (FR-20, FR-30).
 * Dùng database riêng {@code hmm_loyalty_test}, được tạo lại mỗi lần chạy; không đụng dữ liệu demo.
 * Tự bỏ qua nếu máy không kết nối được PostgreSQL (mật khẩu đọc từ local.properties hoặc biến DB_PASSWORD).
 */
@SpringBootTest(properties = "app.seed-demo-data=false")
@EnabledIf("vn.sofitech.hmm.loyalty.LoyaltyIntegrationTest#databaseAvailable")
class LoyaltyIntegrationTest {

    static final String TEST_DB = "hmm_loyalty_test";

    @Autowired JdbcClient jdbc;
    @Autowired TransactionTemplate tx;
    @Autowired LedgerService ledger;
    @Autowired RedemptionService redemptions;
    @Autowired LoyaltyConfigService config;

    // ---------------------------------------------------------------- chuẩn bị CSDL test
    static String password() {
        String env = System.getenv("DB_PASSWORD");
        if (env != null) return env;
        try {
            Properties p = new Properties();
            Path f = Path.of("local.properties");
            if (Files.exists(f)) try (var r = Files.newBufferedReader(f)) { p.load(r); }
            return p.getProperty("DB_PASSWORD", "postgres");
        } catch (IOException e) {
            return "postgres";
        }
    }

    static String host() {
        return System.getenv().getOrDefault("DB_HOST", "localhost") + ":" + System.getenv().getOrDefault("DB_PORT", "5432");
    }

    /** Điều kiện chạy test: kết nối được PostgreSQL; đồng thời tạo lại database test trống. */
    static Boolean available;

    static synchronized boolean databaseAvailable() {
        if (available != null) return available;
        available = false;
        try (Connection c = DriverManager.getConnection("jdbc:postgresql://" + host() + "/postgres",
                System.getenv().getOrDefault("DB_USER", "postgres"), password()); Statement st = c.createStatement()) {
            st.execute("DROP DATABASE IF EXISTS " + TEST_DB + " WITH (FORCE)");
            st.execute("CREATE DATABASE " + TEST_DB + " ENCODING 'UTF8' TEMPLATE template0");
            return available = true;
        } catch (Exception e) {
            System.out.println("Bỏ qua LoyaltyIntegrationTest: không kết nối được PostgreSQL – " + e.getMessage());
            return false;
        }
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> "jdbc:postgresql://" + host() + "/" + TEST_DB);
        r.add("spring.datasource.password", LoyaltyIntegrationTest::password);
    }

    long customerId;

    @BeforeEach
    void setUp() {
        tx.executeWithoutResult(s -> {
            jdbc.sql("TRUNCATE admin.stores, admin.roles, loyalty.tiers, loyalty.loyalty_policies, shared.outbox_messages RESTART IDENTITY CASCADE").update();
            jdbc.sql("INSERT INTO admin.stores (store_id, store_code, name, status) VALUES (1, 'HM001', 'Test', 'ACTIVE')").update();
            jdbc.sql("INSERT INTO admin.pos_terminals (terminal_id, store_id, terminal_code, status) VALUES (1, 1, 'HM001-Q01', 'ACTIVE')").update();
            jdbc.sql("INSERT INTO admin.roles (role_id, code, name) VALUES (1, 'CASHIER', 'Thu ngân')").update();
            jdbc.sql("INSERT INTO admin.app_users (user_id, username, password_hash, full_name, role_id, store_id) VALUES (1, 'tn', 'x', 'Thu ngân', 1, 1)").update();
            jdbc.sql("INSERT INTO loyalty.tiers VALUES ('MEMBER', 'Thành viên', 0, 1.00, NULL), ('SILVER', 'Bạc', 3000000, 1.20, NULL)").update();
            jdbc.sql("""
                    INSERT INTO loyalty.loyalty_policies (version, earn_unit_vnd, point_value_vnd, min_redeem_points, max_redeem_pct, expiry_months,
                        hold_minutes, effective_from, status) VALUES (1, 10000, 100, 50, 50, 12, 15, now() - interval '1 day', 'APPROVED')
                    """).update();
            jdbc.sql("INSERT INTO pos.sales_transactions (transaction_id, receipt_no, store_id, txn_type, status, source, txn_time, net_amount) VALUES (1, 'R1', 1, 'SALE', 'COMPLETED', 'POS', now(), 0), (2, 'R2', 1, 'SALE', 'COMPLETED', 'POS', now(), 0), (3, 'R3', 1, 'SALE', 'COMPLETED', 'POS', now(), 0)").update();
            customerId = jdbc.sql("""
                    INSERT INTO customer.customers (member_code, phone, full_name, status, tier_code, created_at)
                    VALUES ('HM90000001', '0901112222', 'Khách Test', 'UNVERIFIED', 'MEMBER', now()) RETURNING customer_id
                    """).query(Long.class).single();
        });
    }

    PointCalculator.Policy policy() {
        return config.currentPolicy();
    }

    int ledgerSum() {
        return jdbc.sql("SELECT COALESCE(sum(points), 0) FROM loyalty.point_ledgers WHERE customer_id = :c").param("c", customerId).query(Integer.class).single();
    }

    long newRedemption(int points) {
        return jdbc.sql("INSERT INTO loyalty.point_redemptions (customer_id, terminal_id, points, amount_vnd, status, created_at) VALUES (:c, 1, :p, :a, 'QUOTED', now()) RETURNING redemption_id")
                .param("c", customerId).param("p", points).param("a", points * 100L).query(Long.class).single();
    }

    // ---------------------------------------------------------------- sổ cái và FIFO
    @Test
    @DisplayName("FR-33: tích điểm tạo lô điểm và bút toán EARN; số dư = tổng sổ cái")
    void earnCreatesLotAndLedger() {
        tx.executeWithoutResult(s -> ledger.earn(customerId, 1, 120, OffsetDateTime.now(), policy(), 1L));
        assertThat(ledger.balance(customerId)).isEqualTo(120).isEqualTo(ledgerSum());
        Map<String, Object> lot = jdbc.sql("SELECT points_earned, points_remaining, expires_at > now() + interval '10 months' AS ok FROM loyalty.point_lots WHERE customer_id = :c")
                .param("c", customerId).query().singleRow();
        assertThat(lot).containsEntry("points_earned", 120).containsEntry("points_remaining", 120).containsEntry("ok", true);
    }

    @Test
    @DisplayName("FR-30 + BRL-10: giữ điểm rồi đổi điểm trừ lô cũ trước (FIFO)")
    void redeemConsumesOldestLotsFirst() {
        long rid = newRedemption(150);
        tx.executeWithoutResult(s -> {
            ledger.earn(customerId, 1, 100, OffsetDateTime.now().minusDays(30), policy(), 1L);
            ledger.earn(customerId, 2, 80, OffsetDateTime.now().minusDays(5), policy(), 1L);
        });
        tx.executeWithoutResult(s -> assertThat(ledger.hold(customerId, rid, 150, policy(), 1L)).isEqualTo(30));
        tx.executeWithoutResult(s -> assertThat(ledger.redeem(customerId, rid, 3, 150, policy(), 1L)).isEqualTo(30));

        List<Integer> remaining = jdbc.sql("SELECT points_remaining FROM loyalty.point_lots WHERE customer_id = :c ORDER BY earned_at")
                .param("c", customerId).query(Integer.class).list();
        assertThat(remaining).containsExactly(0, 30);
        List<String> entries = jdbc.sql("SELECT entry_type || ':' || points FROM loyalty.point_ledgers WHERE customer_id = :c ORDER BY ledger_id")
                .param("c", customerId).query(String.class).list();
        assertThat(entries).containsExactly("EARN:100", "EARN:80", "HOLD:-150", "RELEASE:150", "REDEEM:-100", "REDEEM:-50");
        assertThat(ledgerSum()).isEqualTo(30).isEqualTo(ledger.balance(customerId));
    }

    @Test
    @DisplayName("Không giữ được điểm khi số dư không đủ; trả điểm (RELEASE) khôi phục số dư")
    void holdAndRelease() {
        long rid = newRedemption(100);
        tx.executeWithoutResult(s -> ledger.earn(customerId, 1, 60, OffsetDateTime.now(), policy(), 1L));
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> ledger.hold(customerId, rid, 100, policy(), 1L)))
                .isInstanceOf(ApiException.class).hasMessageContaining("không đủ");
        tx.executeWithoutResult(s -> {
            ledger.hold(customerId, rid, 60, policy(), 1L);
            ledger.release(customerId, rid, 60, policy(), 1L);
        });
        assertThat(ledger.balance(customerId)).isEqualTo(60).isEqualTo(ledgerSum());
    }

    @Test
    @DisplayName("FR-33: sổ cái chỉ ghi thêm – CSDL chặn UPDATE và DELETE")
    void ledgerIsAppendOnly() {
        tx.executeWithoutResult(s -> ledger.earn(customerId, 1, 10, OffsetDateTime.now(), policy(), 1L));
        assertThatThrownBy(() -> jdbc.sql("UPDATE loyalty.point_ledgers SET points = 999").update()).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbc.sql("DELETE FROM loyalty.point_ledgers").update()).isInstanceOf(DataAccessException.class);
    }

    // ---------------------------------------------------------------- luồng OTP
    String lastOtp() {
        String text = jdbc.sql("SELECT payload->>'text' FROM shared.outbox_messages WHERE message_type = 'SMS_OTP' ORDER BY outbox_id DESC LIMIT 1")
                .query(String.class).single();
        Matcher m = Pattern.compile("(\\d{6})").matcher(text);
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    @Test
    @DisplayName("FR-30 + FR-20: OTP đúng → giữ điểm (HELD) và hồ sơ chuyển Đã xác thực; hủy → trả điểm")
    void otpFlowHoldsPointsAndVerifiesPhone() {
        tx.executeWithoutResult(s -> ledger.earn(customerId, 1, 500, OffsetDateTime.now(), policy(), 1L));
        Map<String, Object> req = redemptions.request(customerId, 1, 200, 400_000);
        long rid = ((Number) req.get("redemptionId")).longValue();
        assertThat(req.get("status")).isEqualTo("QUOTED");

        assertThatThrownBy(() -> redemptions.verifyOtp(rid, 1, "000000".equals(lastOtp()) ? "111111" : "000000", 1))
                .isInstanceOf(ApiException.class).hasMessageContaining("không đúng");
        Map<String, Object> held = redemptions.verifyOtp(rid, 1, lastOtp(), 1);
        assertThat(held).containsEntry("status", "HELD").containsEntry("phoneVerified", true);
        assertThat(ledger.balance(customerId)).isEqualTo(300);
        assertThat(jdbc.sql("SELECT status FROM customer.customers WHERE customer_id = :c").param("c", customerId).query(String.class).single())
                .isEqualTo("VERIFIED");

        redemptions.cancel(rid, 1, 1);
        assertThat(ledger.balance(customerId)).isEqualTo(500).isEqualTo(ledgerSum());
        assertThat(jdbc.sql("SELECT status FROM loyalty.point_redemptions WHERE redemption_id = :r").param("r", rid).query(String.class).single())
                .isEqualTo("RELEASED");
    }

    @Test
    @DisplayName("BRL-09: sai OTP 3 lần → hủy yêu cầu đổi điểm, không trừ điểm")
    void threeWrongOtpsCancelRequest() {
        tx.executeWithoutResult(s -> ledger.earn(customerId, 1, 500, OffsetDateTime.now(), policy(), 1L));
        long rid = ((Number) redemptions.request(customerId, 1, 100, 400_000).get("redemptionId")).longValue();
        String wrong = "000000".equals(lastOtp()) ? "111111" : "000000";
        assertThatThrownBy(() -> redemptions.verifyOtp(rid, 1, wrong, 1)).hasMessageContaining("không đúng");
        assertThatThrownBy(() -> redemptions.verifyOtp(rid, 1, wrong, 1)).hasMessageContaining("không đúng");
        assertThatThrownBy(() -> redemptions.verifyOtp(rid, 1, wrong, 1)).hasMessageContaining("quá số lần");
        assertThat(jdbc.sql("SELECT status FROM loyalty.point_redemptions WHERE redemption_id = :r").param("r", rid).query(String.class).single())
                .isEqualTo("RELEASED");
        assertThat(ledger.balance(customerId)).isEqualTo(500);
    }

    @Test
    @DisplayName("Đổi điểm bị từ chối khi tài khoản đang tạm khóa đổi điểm hoặc vượt mức tối đa")
    void requestRejectedWhenLockedOrOverLimit() {
        tx.executeWithoutResult(s -> ledger.earn(customerId, 1, 500, OffsetDateTime.now(), policy(), 1L));
        assertThatThrownBy(() -> redemptions.request(customerId, 1, 300, 40_000)) // 50% × 40.000đ = 20.000đ = 200 điểm tối đa
                .isInstanceOf(ApiException.class).hasMessageContaining("đến 200");
        jdbc.sql("UPDATE customer.customers SET redeem_locked = true WHERE customer_id = :c").param("c", customerId).update();
        assertThatThrownBy(() -> redemptions.request(customerId, 1, 100, 400_000))
                .isInstanceOf(ApiException.class).hasMessageContaining("tạm khóa");
    }
}
