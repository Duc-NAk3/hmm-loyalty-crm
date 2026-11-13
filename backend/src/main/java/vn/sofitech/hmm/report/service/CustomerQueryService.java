package vn.sofitech.hmm.report.service;

import vn.sofitech.hmm.report.dto.customer.CustomerRow;
import vn.sofitech.hmm.report.dto.customer.Profile;
import vn.sofitech.hmm.report.dto.customer.Stats;
import vn.sofitech.hmm.report.dto.customer.TxnRow;
import vn.sofitech.hmm.report.dto.customer.LedgerRow;
import vn.sofitech.hmm.report.dto.customer.ConsentRow;
import vn.sofitech.hmm.report.dto.customer.TierHistoryRow;
import vn.sofitech.hmm.report.dto.customer.SourceRow;
import vn.sofitech.hmm.report.dto.customer.Customer360View;
import vn.sofitech.hmm.report.dto.customer.LineRow;
import vn.sofitech.hmm.report.dto.customer.PhoneView;
import vn.sofitech.hmm.report.dto.audit.AuditRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sofitech.hmm.customer.CustomerFacade;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.AuditService;
import vn.sofitech.hmm.shared.PageResult;
import vn.sofitech.hmm.shared.Texts;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tìm khách và Customer 360 (FR-22): hồ sơ, hạng và tiến độ lên hạng, số dư và sổ cái điểm, lịch sử giao dịch đến từng
 * dòng hàng ở mọi cửa hàng, consent, lịch sử hạng, nguồn dữ liệu chuyển đổi. JPQL theo tên entity (mô hình đọc).
 * SĐT luôn che một phần; chỉ hiện đầy đủ khi bấm xem và được ghi audit.
 */
@Service
@Transactional(readOnly = true)
public class CustomerQueryService {

    private final EntityManager em;
    private final CustomerFacade customers;
    private final AuditService audit;

    public CustomerQueryService(EntityManager em, CustomerFacade customers, AuditService audit) {
        this.em = em;
        this.customers = customers;
        this.audit = audit;
    }

    /**
     * Tìm khách trên Portal bằng Criteria API (JPA): chỉ thêm điều kiện khi người dùng có lọc, nên không có tham số null
     * và Hibernate luôn biết kiểu dữ liệu (lấy từ field của entity).
     * Module report không import class entity của module khác (ModularityTest chặn), nên lấy entity theo tên
     * qua metamodel – giống cách các truy vấn JPQL khác trong module này dùng tên entity.
     */
    public PageResult<CustomerRow> search(String q, String tier, String status, Long storeId, int page, int size) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        EntityType<?> customer = entity("Customer");

        // 1. Đếm tổng số khách khớp bộ lọc (để phân trang)
        CriteriaQuery<Long> count = cb.createQuery(Long.class);
        Root<?> counted = count.from(customer);
        count.select(cb.count(counted)).where(customerFilters(cb, counted, q, tier, status, storeId));
        long total = em.createQuery(count).getSingleResult();

        // 2. Lấy một trang, sắp theo chi tiêu 12 tháng giảm dần
        CriteriaQuery<Tuple> select = cb.createTupleQuery();
        Root<?> c = select.from(customer);
        select.multiselect(c.get("id"), c.get("memberCode"), c.get("fullName"), c.get("phone"), c.get("tierCode"), c.get("status"),
                        c.get("pointBalance"), c.get("spend12m"), c.get("registeredStoreId"), c.get("createdAt"))
                .where(customerFilters(cb, c, q, tier, status, storeId))
                .orderBy(cb.desc(c.get("spend12m")), cb.asc(c.get("id")));
        List<Tuple> rows = em.createQuery(select).setFirstResult(page * size).setMaxResults(size).getResultList();

        // 3. Tên hạng (4 dòng) và mã cửa hàng (3 dòng) đọc một lần rồi ghép trong Java
        Map<String, String> tierNames = em.createQuery("select t.code, t.name from Tier t", Object[].class).getResultList()
                .stream().collect(Collectors.toMap(r -> (String) r[0], r -> (String) r[1]));
        Map<Long, String> storeCodes = em.createQuery("select s.id, s.storeCode from Store s", Object[].class).getResultList()
                .stream().collect(Collectors.toMap(r -> (Long) r[0], r -> (String) r[1]));

        List<CustomerRow> items = rows.stream().map(r -> new CustomerRow(
                r.get(0, Long.class), r.get(1, String.class), r.get(2, String.class), Texts.maskPhone(r.get(3, String.class)),
                r.get(4, String.class), tierNames.get(r.get(4, String.class)), String.valueOf(r.get(5)),
                r.get(6, Integer.class), r.get(7, Long.class), storeCodes.get(r.get(8, Long.class)), r.get(9, OffsetDateTime.class)))
                .toList();
        return new PageResult<>(total, items);
    }

    /** Điều kiện lọc khách: luôn bỏ hồ sơ đã gộp; các điều kiện khác chỉ thêm khi có giá trị. */
    private static Predicate[] customerFilters(CriteriaBuilder cb, Root<?> c, String q, String tier, String status, Long storeId) {
        List<Predicate> where = new ArrayList<>();
        where.add(cb.notEqual(c.get("status").as(String.class), "MERGED"));
        if (!Texts.isBlank(q)) {
            String keyword = q.trim();
            List<Predicate> anyOf = new ArrayList<>();
            anyOf.add(cb.equal(c.get("memberCode"), keyword.toUpperCase()));                 // mã thành viên HM…
            String phone = Texts.normalizePhone(keyword);
            if (phone != null) anyOf.add(cb.equal(c.get("phone"), phone));                   // SĐT (+84/84/0)
            anyOf.add(cb.like(c.get("nameSearch"), "%" + Texts.unaccent(keyword) + "%"));   // họ tên không dấu
            where.add(cb.or(anyOf.toArray(Predicate[]::new)));
        }
        if (!Texts.isBlank(tier)) where.add(cb.equal(c.get("tierCode"), tier));
        if (!Texts.isBlank(status)) where.add(cb.equal(c.get("status").as(String.class), status));
        if (storeId != null) where.add(cb.equal(c.get("registeredStoreId"), storeId));
        return where.toArray(Predicate[]::new);
    }

    /** Kiểu entity theo tên (ví dụ "Customer") trong metamodel của JPA. */
    private EntityType<?> entity(String name) {
        return em.getMetamodel().getEntities().stream().filter(e -> e.getName().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Không có entity " + name));
    }

    public Customer360View view(long customerId) {
        List<Object[]> rows = em.createQuery("""
                select c.id, c.memberCode, c.fullName, c.phone, c.gender, c.dateOfBirth, c.email, c.status, c.verifiedAt,
                       c.tierCode, tr.name, tr.multiplier, c.pointBalance, c.spend12m, c.redeemLocked, c.employee,
                       s.storeCode, s.name, c.createdAt, c.mergedIntoId, tr.thresholdSpend
                from Customer c join Tier tr on tr.code = c.tierCode left join Store s on s.id = c.registeredStoreId
                where c.id = :id
                """, Object[].class).setParameter("id", customerId).getResultList();
        if (rows.isEmpty()) throw ApiException.notFound("Không tìm thấy khách hàng");
        Object[] r = rows.getFirst();
        long threshold = (Long) r[20];
        List<Object[]> next = em.createQuery("select n.name, n.thresholdSpend from Tier n where n.thresholdSpend > :t order by n.thresholdSpend",
                Object[].class).setParameter("t", threshold).setMaxResults(1).getResultList();
        Profile profile = new Profile((Long) r[0], (String) r[1], (String) r[2], Texts.maskPhone((String) r[3]), (String) r[4],
                (LocalDate) r[5], (String) r[6], String.valueOf(r[7]), (OffsetDateTime) r[8], (String) r[9], (String) r[10],
                (BigDecimal) r[11], (Integer) r[12], (Long) r[13], (Boolean) r[14], (Boolean) r[15], (String) r[16], (String) r[17],
                (OffsetDateTime) r[18], (Long) r[19], next.isEmpty() ? null : (String) next.getFirst()[0],
                next.isEmpty() ? null : (Long) next.getFirst()[1], threshold);

        OffsetDateTime now = OffsetDateTime.now(Texts.VN);
        Object[] t = em.createQuery("""
                select count(t), coalesce(sum(t.netAmount), 0), max(t.txnTime), count(distinct t.storeId)
                from SalesTransaction t where t.customerId = :id and t.status = vn.sofitech.hmm.pos.domain.SalesTransaction.Status.COMPLETED
                """, Object[].class).setParameter("id", customerId).getSingleResult();
        Stats stats = new Stats(n(t[0]), n(t[1]), (OffsetDateTime) t[2], n(t[3]),
                single("select coalesce(sum(l.pointsEarned), 0) from PointLot l where l.customerId = :id", customerId),
                -single("select coalesce(sum(g.points), 0) from PointLedger g where g.customerId = :id"
                        + " and g.entryType = vn.sofitech.hmm.loyalty.domain.PointLedger.EntryType.REDEEM", customerId),
                single("select coalesce(sum(g.points), 0) from PointLedger g where g.customerId = :id", customerId),
                n(em.createQuery("""
                        select coalesce(sum(l.pointsRemaining), 0) from PointLot l where l.customerId = :id and l.pointsRemaining > 0
                          and l.expiresAt > :now and l.expiresAt <= :until
                        """).setParameter("id", customerId).setParameter("now", now).setParameter("until", now.plusDays(30)).getSingleResult()),
                (OffsetDateTime) em.createQuery("select min(l.expiresAt) from PointLot l where l.customerId = :id and l.pointsRemaining > 0"
                        + " and l.expiresAt > :now").setParameter("id", customerId).setParameter("now", now).getSingleResult());

        List<TxnRow> txns = em.createQuery("""
                        select t.id, t.receiptNo, t.txnTime, s.storeCode, s.name, t.txnType, t.netAmount, t.pointsEarned, t.pointsRedeemed,
                               t.source, size(t.lines)
                        from SalesTransaction t join Store s on s.id = t.storeId
                        where t.customerId = :id order by t.txnTime desc
                        """, Object[].class).setParameter("id", customerId).setMaxResults(100).getResultList().stream()
                .map(x -> new TxnRow((Long) x[0], (String) x[1], (OffsetDateTime) x[2], (String) x[3], (String) x[4], String.valueOf(x[5]),
                        (Long) x[6], (Integer) x[7], (Integer) x[8], String.valueOf(x[9]), n(x[10]))).toList();

        List<LedgerRow> ledger = em.createQuery("""
                        select g.id, g.createdAt, g.entryType, g.points, g.balanceAfter, t.receiptNo, g.redemption.id, l.expiresAt
                        from PointLedger g left join g.lot l left join SalesTransaction t on t.id = g.transactionId
                        where g.customerId = :id order by g.id desc
                        """, Object[].class).setParameter("id", customerId).setMaxResults(200).getResultList().stream()
                .map(x -> new LedgerRow((Long) x[0], (OffsetDateTime) x[1], String.valueOf(x[2]), (Integer) x[3], (Integer) x[4],
                        (String) x[5], (Long) x[6], (OffsetDateTime) x[7])).toList();

        List<ConsentRow> consents = em.createQuery("""
                        select k.purpose, k.status, k.channel, k.termsVersion, k.capturedAt, u.fullName
                        from Consent k left join AppUser u on u.id = k.capturedBy
                        where k.customer.id = :id order by k.capturedAt desc
                        """, Object[].class).setParameter("id", customerId).getResultList().stream()
                .map(x -> new ConsentRow(String.valueOf(x[0]), String.valueOf(x[1]), (String) x[2], (String) x[3], (OffsetDateTime) x[4],
                        (String) x[5])).toList();

        List<TierHistoryRow> tierHistory = em.createQuery("""
                        select h.changedAt, h.fromTier, h.toTier, h.reason from TierHistory h where h.customerId = :id order by h.changedAt desc
                        """, Object[].class).setParameter("id", customerId).getResultList().stream()
                .map(x -> new TierHistoryRow((OffsetDateTime) x[0], (String) x[1], (String) x[2], String.valueOf(x[3]))).toList();

        List<SourceRow> sources = em.createQuery("""
                        select r.sourceVendor, r.sourceCustomerRef, b.id, b.importedAt, s.storeCode
                        from CustomerSourceRecord r join r.batch b join Store s on s.id = b.storeId
                        where r.customerId = :id order by r.id
                        """, Object[].class).setParameter("id", customerId).getResultList().stream()
                .map(x -> new SourceRow((String) x[0], (String) x[1], (Long) x[2], (OffsetDateTime) x[3], (String) x[4])).toList();

        return new Customer360View(profile, stats, txns, ledger, consents, tierHistory, sources);
    }

    public List<LineRow> transactionLines(long customerId, long transactionId) {
        Long owner = em.createQuery("select count(t) from SalesTransaction t where t.id = :t and t.customerId = :c", Long.class)
                .setParameter("t", transactionId).setParameter("c", customerId).getSingleResult();
        if (owner == 0) throw ApiException.notFound("Không tìm thấy hóa đơn của khách");
        return em.createQuery("""
                        select p.sku, p.name, c.name, l.quantity, l.unitPrice, l.lineAmount, l.pointEligible
                        from TransactionLine l join Product p on p.id = l.productId join p.category c
                        where l.transaction.id = :t order by l.id
                        """, Object[].class).setParameter("t", transactionId).getResultList().stream()
                .map(x -> new LineRow((String) x[0], (String) x[1], (String) x[2], (BigDecimal) x[3], (Long) x[4], (Long) x[5], (Boolean) x[6]))
                .toList();
    }

    @Transactional
    public PhoneView revealPhone(long customerId, long userId) {
        String phone = customers.findById(customerId).map(CustomerFacade.CustomerInfo::phone)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy khách hàng"));
        audit.log(userId, "VIEW_FULL_PHONE", "customers", customerId, null, null);
        return new PhoneView(phone);
    }

    public List<AuditRow> auditLogs() {
        return em.createQuery("""
                        select a.id, a.createdAt, u.username, a.action, a.entityType, a.entityId, a.afterData
                        from AuditLog a left join AppUser u on u.id = a.userId order by a.id desc
                        """, Object[].class).setMaxResults(200).getResultList().stream()
                .map(x -> new AuditRow((Long) x[0], (OffsetDateTime) x[1], (String) x[2], (String) x[3], (String) x[4], (String) x[5],
                        (String) x[6])).toList();
    }

    private long single(String jpql, long customerId) {
        return n(em.createQuery(jpql).setParameter("id", customerId).getSingleResult());
    }

    private static long n(Object o) {
        return o == null ? 0 : ((Number) o).longValue();
    }
}
