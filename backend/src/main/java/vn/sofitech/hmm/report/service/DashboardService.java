package vn.sofitech.hmm.report.service;

import vn.sofitech.hmm.report.dto.dashboard.Kpi;
import vn.sofitech.hmm.report.dto.dashboard.TierCount;
import vn.sofitech.hmm.report.dto.dashboard.StoreRevenue;
import vn.sofitech.hmm.report.dto.dashboard.DayRevenue;
import vn.sofitech.hmm.report.dto.dashboard.CategoryRevenue;
import vn.sofitech.hmm.report.dto.dashboard.DashboardView;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static vn.sofitech.hmm.shared.Texts.VN;

/**
 * Dashboard KPI (FR-51). Module report là mô hình đọc: truy vấn tổng hợp bằng JPQL theo tên entity của các module khác
 * (không phụ thuộc kiểu Java của chúng, nên không phá ranh giới module).
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    /** Điều kiện chung cho hóa đơn bán hoàn tất trong kỳ, lọc theo cửa hàng nếu có. */
    private static final String SALE = """
            t.txnType = vn.sofitech.hmm.pos.domain.SalesTransaction.Type.SALE
            and t.status = vn.sofitech.hmm.pos.domain.SalesTransaction.Status.COMPLETED
            and t.txnTime >= :start and t.txnTime < :end and (:store is null or t.storeId = :store)
            """;
    private static final String NOT_MERGED = "c.status <> vn.sofitech.hmm.customer.domain.Customer.Status.MERGED";

    private final EntityManager em;

    public DashboardService(EntityManager em) {
        this.em = em;
    }

    public DashboardView dashboard(LocalDate from, LocalDate to, Long storeId) {
        OffsetDateTime start = from.atStartOfDay(VN).toOffsetDateTime();
        OffsetDateTime end = to.plusDays(1).atStartOfDay(VN).toOffsetDateTime();

        Object[] k = period(em.createQuery("""
                select count(t), coalesce(sum(t.netAmount), 0), count(t.customerId),
                       coalesce(sum(case when t.customerId is not null then t.netAmount else 0 end), 0),
                       count(distinct t.customerId), coalesce(sum(t.pointsEarned), 0), coalesce(sum(t.pointsRedeemed), 0)
                from SalesTransaction t where
                """ + SALE, Object[].class), start, end, storeId).getSingleResult();
        long receipts = n(k[0]), revenue = n(k[1]), memberReceipts = n(k[2]), memberRevenue = n(k[3]);

        // Tỷ lệ quay lại: thành viên mua ≥ 2 lần trong kỳ / thành viên có mua trong kỳ
        List<Long> visits = period(em.createQuery("select count(t) from SalesTransaction t where t.customerId is not null and "
                + SALE + " group by t.customerId", Long.class), start, end, storeId).getResultList();
        long returning = visits.stream().filter(v -> v >= 2).count();

        Object[] m = em.createQuery("select count(c), coalesce(sum(case when c.createdAt >= :start and c.createdAt < :end then 1 else 0 end), 0),"
                        + " coalesce(sum(case when c.status = vn.sofitech.hmm.customer.domain.Customer.Status.VERIFIED then 1 else 0 end), 0)"
                        + " from Customer c where " + NOT_MERGED + " and (:store is null or c.registeredStoreId = :store)", Object[].class)
                .setParameter("start", start).setParameter("end", end).setParameter("store", storeId).getSingleResult();

        Kpi kpi = new Kpi(receipts, revenue, memberReceipts, memberRevenue, n(k[4]), n(k[5]), n(k[6]),
                pct(memberReceipts, receipts), pct(memberRevenue, revenue), receipts == 0 ? 0 : revenue / receipts,
                pct(returning, visits.size()), n(m[0]), n(m[1]), n(m[2]));

        List<TierCount> tiers = em.createQuery("""
                        select tr.code, tr.name,
                               (select count(c) from Customer c where c.tierCode = tr.code and %s and (:store is null or c.registeredStoreId = :store))
                        from Tier tr order by tr.thresholdSpend
                        """.formatted(NOT_MERGED), Object[].class).setParameter("store", storeId).getResultList().stream()
                .map(r -> new TierCount((String) r[0], (String) r[1], n(r[2]))).toList();

        List<StoreRevenue> byStore = period(em.createQuery("""
                        select s.storeCode, s.name, count(t), coalesce(sum(t.netAmount), 0),
                               coalesce(sum(case when t.customerId is not null then t.netAmount else 0 end), 0), count(t.customerId)
                        from Store s left join SalesTransaction t on t.storeId = s.id and
                        """ + SALE + " where (:store is null or s.id = :store) group by s.storeCode, s.name order by s.storeCode",
                Object[].class), start, end, storeId).getResultList().stream()
                .map(r -> new StoreRevenue((String) r[0], (String) r[1], n(r[2]), n(r[3]), n(r[4]), n(r[5]))).toList();

        List<DayRevenue> byDay = period(em.createQuery("""
                        select cast(t.txnTime as LocalDate),
                               coalesce(sum(case when t.customerId is not null then t.netAmount else 0 end), 0),
                               coalesce(sum(case when t.customerId is null then t.netAmount else 0 end), 0), count(t), count(t.customerId)
                        from SalesTransaction t where
                        """ + SALE + " group by cast(t.txnTime as LocalDate) order by cast(t.txnTime as LocalDate)", Object[].class),
                start, end, storeId).getResultList().stream()
                .map(r -> new DayRevenue((LocalDate) r[0], n(r[1]), n(r[2]), n(r[3]), n(r[4]))).toList();

        List<CategoryRevenue> categories = period(em.createQuery("""
                        select c.name, coalesce(sum(l.lineAmount), 0)
                        from TransactionLine l join l.transaction t join Product p on p.id = l.productId join p.category c
                        where
                        """ + SALE + " group by c.name order by coalesce(sum(l.lineAmount), 0) desc", Object[].class),
                start, end, storeId).setMaxResults(8).getResultList().stream()
                .map(r -> new CategoryRevenue((String) r[0], n(r[1]))).toList();

        return new DashboardView(from, to, storeId, kpi, tiers, byStore, byDay, categories);
    }

    private static <T> TypedQuery<T> period(TypedQuery<T> q, OffsetDateTime start, OffsetDateTime end, Long storeId) {
        return q.setParameter("start", start).setParameter("end", end).setParameter("store", storeId);
    }

    private static long n(Object o) {
        return o == null ? 0 : ((Number) o).longValue();
    }

    private static double pct(long part, long whole) {
        return whole == 0 ? 0 : Math.round(1000.0 * part / whole) / 10.0;
    }
}
