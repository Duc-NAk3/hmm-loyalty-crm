package vn.sofitech.hmm.loyalty;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import vn.sofitech.hmm.PostgresTestBase;
import vn.sofitech.hmm.admin.domain.*;
import vn.sofitech.hmm.customer.CustomerFacade;
import vn.sofitech.hmm.loyalty.LoyaltyFacade.RedemptionHeld;
import vn.sofitech.hmm.loyalty.LoyaltyFacade.RedemptionRequested;
import vn.sofitech.hmm.loyalty.domain.*;
import vn.sofitech.hmm.loyalty.service.LedgerService;
import vn.sofitech.hmm.loyalty.service.LoyaltyConfigService;
import vn.sofitech.hmm.pos.domain.SalesTransaction;
import vn.sofitech.hmm.pos.domain.SalesTransactionRepository;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.domain.OutboxMessageRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm tra tích hợp với PostgreSQL thật qua ORM (JPA): sổ cái điểm (FR-33), giữ / trả / đổi điểm FIFO và luồng OTP (FR-20, FR-30).
 */
@EnabledIf("vn.sofitech.hmm.PostgresTestBase#databaseAvailable")
class LoyaltyIntegrationTest extends PostgresTestBase {

    @Autowired TransactionTemplate tx;
    @Autowired EntityManager em;
    @Autowired LedgerService ledger;
    @Autowired LoyaltyFacade loyalty;
    @Autowired LoyaltyConfigService config;
    @Autowired CustomerFacade customers;
    @Autowired StoreRepository stores;
    @Autowired PosTerminalRepository terminals;
    @Autowired RoleRepository roles;
    @Autowired AppUserRepository users;
    @Autowired TierRepository tiers;
    @Autowired LoyaltyPolicyRepository policies;
    @Autowired PointLotRepository lots;
    @Autowired PointLedgerRepository ledgers;
    @Autowired PointRedemptionRepository redemptions;
    @Autowired SalesTransactionRepository transactions;
    @Autowired OutboxMessageRepository outbox;

    long customerId;
    long terminalId;
    long cashierId;
    long txn1, txn2, txn3;

    @BeforeEach
    void setUp() {
        tx.executeWithoutResult(s -> {
            // TRUNCATE bỏ qua trigger chỉ-ghi-thêm của sổ cái; chỉ dùng để dọn dữ liệu test
            em.createNativeQuery("TRUNCATE admin.stores, admin.roles, loyalty.tiers, loyalty.loyalty_policies, shared.outbox_messages CASCADE")
                    .executeUpdate();
            Store store = stores.save(new Store("HM001", "Test", null, null, null));
            terminalId = terminals.save(new PosTerminal(store, "HM001-Q01", OffsetDateTime.now())).getId();
            cashierId = users.save(new AppUser("tn", "x", "Thu ngân", null, roles.save(new Role("CASHIER", "Thu ngân")), store)).getId();
            tiers.save(new Tier("MEMBER", "Thành viên", 0, new BigDecimal("1.00"), null));
            tiers.save(new Tier("SILVER", "Bạc", 3_000_000, new BigDecimal("1.20"), null));
            policies.save(new LoyaltyPolicy(1, 10_000, 100, 50, 50, 12, 15, OffsetDateTime.now().minusDays(1), null));
            txn1 = transactions.save(SalesTransaction.sale("R1", store.getId(), terminalId, null, null, null, OffsetDateTime.now())).getId();
            txn2 = transactions.save(SalesTransaction.sale("R2", store.getId(), terminalId, null, null, null, OffsetDateTime.now())).getId();
            txn3 = transactions.save(SalesTransaction.sale("R3", store.getId(), terminalId, null, null, null, OffsetDateTime.now())).getId();
            customerId = customers.create(new CustomerFacade.NewCustomer("0901112222", "Khách Test", null, null, null, store.getId(),
                    "UNVERIFIED", null));
        });
    }

    PointCalculator.Policy policy() {
        return config.currentPolicy();
    }

    int balance() {
        return customers.findById(customerId).orElseThrow().pointBalance();
    }

    long ledgerSum() {
        return ledgers.sumPoints(customerId);
    }

    PointRedemption newRedemption(int points) {
        return redemptions.save(new PointRedemption(customerId, terminalId, points, points * 100L, OffsetDateTime.now()));
    }

    // ---------------------------------------------------------------- sổ cái và FIFO
    @Test
    @DisplayName("FR-33: tích điểm tạo lô điểm và bút toán EARN; số dư = tổng sổ cái")
    void earnCreatesLotAndLedger() {
        tx.executeWithoutResult(s -> ledger.earn(customerId, txn1, 120, OffsetDateTime.now(), policy(), cashierId));
        assertThat((long) balance()).isEqualTo(120).isEqualTo(ledgerSum());
        PointLot lot = lots.findByCustomerIdOrderByEarnedAt(customerId).getFirst();
        assertThat(lot.getPointsEarned()).isEqualTo(120);
        assertThat(lot.getPointsRemaining()).isEqualTo(120);
        assertThat(lot.getExpiresAt()).isAfter(OffsetDateTime.now().plusMonths(10));
    }

    @Test
    @DisplayName("FR-30 + BRL-10: giữ điểm rồi đổi điểm trừ lô cũ trước (FIFO)")
    void redeemConsumesOldestLotsFirst() {
        tx.executeWithoutResult(s -> {
            ledger.earn(customerId, txn1, 100, OffsetDateTime.now().minusDays(30), policy(), cashierId);
            ledger.earn(customerId, txn2, 80, OffsetDateTime.now().minusDays(5), policy(), cashierId);
        });
        tx.executeWithoutResult(s -> {
            PointRedemption r = newRedemption(150);
            assertThat(ledger.hold(customerId, r, 150, policy(), cashierId)).isEqualTo(30);
            assertThat(ledger.redeem(customerId, r, txn3, 150, policy(), cashierId)).isEqualTo(30);
        });
        assertThat(lots.findByCustomerIdOrderByEarnedAt(customerId)).extracting(PointLot::getPointsRemaining).containsExactly(0, 30);
        List<String> entries = ledgers.findByCustomerIdOrderById(customerId).stream()
                .map(g -> g.getEntryType() + ":" + g.getPoints()).toList();
        assertThat(entries).containsExactly("EARN:100", "EARN:80", "HOLD:-150", "RELEASE:150", "REDEEM:-100", "REDEEM:-50");
        assertThat(ledgerSum()).isEqualTo(30).isEqualTo(balance());
    }

    @Test
    @DisplayName("Không giữ được điểm khi số dư không đủ; trả điểm (RELEASE) khôi phục số dư")
    void holdAndRelease() {
        tx.executeWithoutResult(s -> ledger.earn(customerId, txn1, 60, OffsetDateTime.now(), policy(), cashierId));
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> ledger.hold(customerId, newRedemption(100), 100, policy(), cashierId)))
                .isInstanceOf(ApiException.class).hasMessageContaining("không đủ");
        tx.executeWithoutResult(s -> {
            PointRedemption r = newRedemption(60);
            ledger.hold(customerId, r, 60, policy(), cashierId);
            ledger.release(customerId, r, 60, policy(), cashierId);
        });
        assertThat((long) balance()).isEqualTo(60).isEqualTo(ledgerSum());
    }

    @Test
    @DisplayName("FR-33: sổ cái chỉ ghi thêm – CSDL chặn UPDATE và DELETE")
    void ledgerIsAppendOnly() {
        tx.executeWithoutResult(s -> ledger.earn(customerId, txn1, 10, OffsetDateTime.now(), policy(), cashierId));
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> em.createNativeQuery("UPDATE loyalty.point_ledgers SET points = 999").executeUpdate()))
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> em.createNativeQuery("DELETE FROM loyalty.point_ledgers").executeUpdate()))
                .isInstanceOf(RuntimeException.class);
    }

    // ---------------------------------------------------------------- luồng OTP (qua LoyaltyFacade)
    String lastOtp() {
        String payload = outbox.findAll().stream().filter(m -> m.getMessageType().equals("SMS_OTP"))
                .max((a, b) -> Long.compare(a.getId(), b.getId())).orElseThrow().getPayload();
        Matcher m = Pattern.compile("(\\d{6})").matcher(payload);
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    String wrongOtp() {
        return "000000".equals(lastOtp()) ? "111111" : "000000";
    }

    @Test
    @DisplayName("FR-30 + FR-20: OTP đúng → giữ điểm (HELD) và hồ sơ chuyển Đã xác thực; hủy → trả điểm")
    void otpFlowHoldsPointsAndVerifiesPhone() {
        tx.executeWithoutResult(s -> ledger.earn(customerId, txn1, 500, OffsetDateTime.now(), policy(), cashierId));
        RedemptionRequested req = loyalty.requestRedemption(customerId, terminalId, 200, 400_000);
        assertThat(req.status()).isEqualTo("QUOTED");

        assertThatThrownBy(() -> loyalty.verifyOtp(req.redemptionId(), terminalId, wrongOtp(), cashierId))
                .isInstanceOf(ApiException.class).hasMessageContaining("không đúng");
        RedemptionHeld held = loyalty.verifyOtp(req.redemptionId(), terminalId, lastOtp(), cashierId);
        assertThat(held.status()).isEqualTo("HELD");
        assertThat(held.phoneVerified()).isTrue();
        assertThat(balance()).isEqualTo(300);
        assertThat(customers.findById(customerId).orElseThrow().status()).isEqualTo("VERIFIED");

        loyalty.cancelRedemption(req.redemptionId(), terminalId, cashierId);
        assertThat((long) balance()).isEqualTo(500).isEqualTo(ledgerSum());
        assertThat(redemptions.findById(req.redemptionId()).orElseThrow().getStatus()).isEqualTo(PointRedemption.Status.RELEASED);
    }

    @Test
    @DisplayName("BRL-09: sai OTP 3 lần → hủy yêu cầu đổi điểm, không trừ điểm")
    void threeWrongOtpsCancelRequest() {
        tx.executeWithoutResult(s -> ledger.earn(customerId, txn1, 500, OffsetDateTime.now(), policy(), cashierId));
        long id = loyalty.requestRedemption(customerId, terminalId, 100, 400_000).redemptionId();
        String wrong = wrongOtp();
        assertThatThrownBy(() -> loyalty.verifyOtp(id, terminalId, wrong, cashierId)).hasMessageContaining("không đúng");
        assertThatThrownBy(() -> loyalty.verifyOtp(id, terminalId, wrong, cashierId)).hasMessageContaining("không đúng");
        assertThatThrownBy(() -> loyalty.verifyOtp(id, terminalId, wrong, cashierId)).hasMessageContaining("quá số lần");
        assertThat(redemptions.findById(id).orElseThrow().getStatus()).isEqualTo(PointRedemption.Status.RELEASED);
        assertThat(balance()).isEqualTo(500);
    }

    @Test
    @DisplayName("Đổi điểm bị từ chối khi vượt mức tối đa hoặc tài khoản đang tạm khóa đổi điểm")
    void requestRejectedWhenLockedOrOverLimit() {
        tx.executeWithoutResult(s -> ledger.earn(customerId, txn1, 500, OffsetDateTime.now(), policy(), cashierId));
        assertThatThrownBy(() -> loyalty.requestRedemption(customerId, terminalId, 300, 40_000)) // 50% × 40.000đ = 200 điểm tối đa
                .isInstanceOf(ApiException.class).hasMessageContaining("đến 200");
        tx.executeWithoutResult(s -> em.createQuery("update Customer c set c.redeemLocked = true where c.id = :id")
                .setParameter("id", customerId).executeUpdate());
        assertThatThrownBy(() -> loyalty.requestRedemption(customerId, terminalId, 100, 400_000))
                .isInstanceOf(ApiException.class).hasMessageContaining("tạm khóa");
    }
}
