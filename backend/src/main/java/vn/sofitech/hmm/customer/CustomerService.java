package vn.sofitech.hmm.customer;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.OutboxService;
import vn.sofitech.hmm.shared.Texts;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Hồ sơ thành viên: tra cứu, đăng ký, xác thực SĐT, consent (FR-18, FR-19, FR-20). */
@Service
public class CustomerService {

    public static final String TERMS_VERSION = "v1.0";

    public record Customer(long customerId, String memberCode, String phone, String fullName, String status,
                           String tierCode, int pointBalance, long spend12m, boolean redeemLocked, boolean employee) {
    }

    public record NewCustomer(String phone, String fullName, String gender, LocalDate dateOfBirth, String email,
                              Long storeId, String status, OffsetDateTime createdAt) {
    }

    private final JdbcClient jdbc;
    private final OutboxService outbox;

    public CustomerService(JdbcClient jdbc, OutboxService outbox) {
        this.jdbc = jdbc;
        this.outbox = outbox;
    }

    private static final String SELECT = """
            SELECT customer_id, member_code, phone, full_name, status, tier_code, point_balance, spend_12m, redeem_locked, is_employee
            FROM customer.customers
            """;

    private Customer map(java.sql.ResultSet rs, int i) throws java.sql.SQLException {
        return new Customer(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                rs.getString(6), rs.getInt(7), rs.getLong(8), rs.getBoolean(9), rs.getBoolean(10));
    }

    public Optional<Customer> findById(long id) {
        return jdbc.sql(SELECT + " WHERE customer_id = :id").param("id", id).query(this::map).optional();
    }

    /** Tra cứu theo SĐT (tự chuẩn hóa +84/84) hoặc mã thành viên; bỏ qua hồ sơ đã gộp. */
    public Optional<Customer> lookup(String query) {
        if (Texts.isBlank(query)) return Optional.empty();
        String q = query.trim().toUpperCase();
        if (q.startsWith("HM"))
            return jdbc.sql(SELECT + " WHERE member_code = :c AND status <> 'MERGED'").param("c", q).query(this::map).optional();
        String phone = Texts.normalizePhone(q);
        if (phone == null) throw ApiException.badRequest("BAD_PHONE", "SĐT không hợp lệ");
        return findActiveByPhone(phone);
    }

    public Optional<Customer> findActiveByPhone(String normalizedPhone) {
        return jdbc.sql(SELECT + " WHERE phone = :p AND status <> 'MERGED'").param("p", normalizedPhone).query(this::map).optional();
    }

    /** Tạo hồ sơ: dùng chung cho đăng ký tại quầy, chuyển đổi dữ liệu và dữ liệu mẫu. */
    @Transactional
    public long create(NewCustomer c) {
        String phone = Texts.normalizePhone(c.phone());
        if (phone == null) throw ApiException.badRequest("BAD_PHONE", "SĐT không hợp lệ");
        if (Texts.isBlank(c.fullName())) throw ApiException.badRequest("NAME", "Họ tên là bắt buộc");
        if (findActiveByPhone(phone).isPresent()) throw ApiException.conflict("PHONE_EXISTS", "SĐT đã là thành viên");
        String name = Texts.normalizeName(c.fullName());
        return jdbc.sql("""
                INSERT INTO customer.customers (member_code, phone, full_name, name_search, gender, date_of_birth, email, status,
                    tier_code, point_balance, spend_12m, is_employee, redeem_locked, registered_store_id, created_at, updated_at)
                VALUES ('HM' || nextval('customer.member_code_seq'), :phone, :name, :search, :gender, :dob, :email, :status,
                    'MEMBER', 0, 0, false, false, :store, :created, :created)
                RETURNING customer_id
                """).param("phone", phone).param("name", name).param("search", Texts.unaccent(name))
                .param("gender", c.gender()).param("dob", c.dateOfBirth()).param("email", Texts.isBlank(c.email()) ? null : c.email().trim())
                .param("status", c.status() == null ? "UNVERIFIED" : c.status()).param("store", c.storeId())
                .param("created", c.createdAt() == null ? OffsetDateTime.now(Texts.VN) : c.createdAt())
                .query(Long.class).single();
    }

    public void addConsent(long customerId, String purpose, String status, String channel, Long capturedBy, OffsetDateTime at) {
        jdbc.sql("""
                INSERT INTO customer.consents (customer_id, purpose, status, channel, terms_version, captured_by, captured_at)
                VALUES (:c, :p, :s, :ch, :v, :by, :at)
                """).param("c", customerId).param("p", purpose).param("s", status).param("ch", channel).param("v", TERMS_VERSION)
                .param("by", capturedBy).param("at", at == null ? OffsetDateTime.now(Texts.VN) : at).update();
    }

    /** Đăng ký nhanh tại quầy (FR-19): không OTP, hồ sơ Chưa xác thực, ghi consent, gửi SMS chào mừng. */
    @Transactional
    public Customer registerAtPos(String phone, String fullName, String gender, LocalDate dob, boolean marketingConsent,
                                  long storeId, long cashierId) {
        long id = create(new NewCustomer(phone, fullName, gender, dob, null, storeId, "UNVERIFIED", null));
        addConsent(id, "MEMBERSHIP", "GRANTED", "POS", cashierId, null);
        if (marketingConsent) addConsent(id, "MARKETING_SMS", "GRANTED", "POS", cashierId, null);
        Customer c = findById(id).orElseThrow();
        outbox.enqueueSms("SMS_WELCOME", "customers", id, c.phone(),
                "Chao mung " + Texts.unaccent(c.fullName()).toUpperCase() + " den voi Hoa Mai Mart! Ma thanh vien cua ban: "
                        + c.memberCode() + ". Tich diem moi lan mua sam.");
        return c;
    }

    /** OTP thành công lần đầu → hồ sơ Đã xác thực (FR-20, BRL-18). */
    public boolean markVerified(long customerId) {
        return jdbc.sql("UPDATE customer.customers SET status = 'VERIFIED', verified_at = now(), updated_at = now() WHERE customer_id = :id AND status = 'UNVERIFIED'")
                .param("id", customerId).update() > 0;
    }

    public Map<String, Object> search(String q, String tier, String status, Long storeId, int page, int size) {
        String query = Texts.isBlank(q) ? null : q.trim();
        String phone = query == null ? null : Texts.normalizePhone(query);
        String where = """
                WHERE c.status <> 'MERGED'
                  AND (CAST(:q AS varchar) IS NULL OR c.member_code = upper(:q) OR c.phone = :phone OR c.name_search LIKE '%' || :search || '%')
                  AND (CAST(:tier AS varchar) IS NULL OR c.tier_code = :tier)
                  AND (CAST(:status AS varchar) IS NULL OR c.status = :status)
                  AND (CAST(:store AS bigint) IS NULL OR c.registered_store_id = :store)
                """;
        String search = query == null ? null : Texts.unaccent(query);
        long total = jdbc.sql("SELECT count(*) FROM customer.customers c " + where)
                .param("q", query).param("phone", phone).param("search", search).param("tier", tier).param("status", status).param("store", storeId)
                .query(Long.class).single();
        List<Map<String, Object>> items = jdbc.sql("""
                SELECT c.customer_id, c.member_code, c.full_name, c.phone, c.tier_code, t.name AS tier_name, c.status,
                       c.point_balance, c.spend_12m, s.store_code AS registered_store, c.created_at
                FROM customer.customers c JOIN loyalty.tiers t ON t.tier_code = c.tier_code
                LEFT JOIN admin.stores s ON s.store_id = c.registered_store_id
                """ + where + " ORDER BY c.spend_12m DESC, c.customer_id LIMIT :size OFFSET :offset")
                .param("q", query).param("phone", phone).param("search", search).param("tier", tier).param("status", status).param("store", storeId)
                .param("size", size).param("offset", page * size).query().listOfRows();
        items.forEach(r -> r.put("phone", Texts.maskPhone((String) r.get("phone"))));
        return Map.of("total", total, "items", items);
    }
}
