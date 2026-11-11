package vn.sofitech.hmm.loyalty;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.sofitech.hmm.shared.ApiException;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Sổ cái điểm (FR-33): mọi biến động là một bản ghi chỉ ghi thêm; customers.point_balance luôn bằng tổng sổ cái.
 * <ul>
 *   <li>EARN: +điểm, tạo một lô điểm (point_lots) để trừ và hết hạn theo FIFO.</li>
 *   <li>HOLD: −điểm khi OTP đúng (điểm bị giữ không còn khả dụng).</li>
 *   <li>RELEASE: +điểm trả lại khi hủy, quá hạn, hoặc ngay trước khi ghi REDEEM.</li>
 *   <li>REDEEM: −điểm theo từng lô FIFO khi hóa đơn hoàn tất.</li>
 * </ul>
 * Các hàm phải chạy trong giao dịch của nghiệp vụ gọi tới (MANDATORY).
 */
@Service
public class LedgerService {

    private final JdbcClient jdbc;

    public LedgerService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public int earn(long customerId, long transactionId, int points, OffsetDateTime earnedAt, PointCalculator.Policy policy, Long userId) {
        if (points <= 0) return balance(customerId);
        long lotId = jdbc.sql("""
                INSERT INTO loyalty.point_lots (customer_id, transaction_id, earned_at, expires_at, points_earned, points_remaining)
                VALUES (:c, :t, :at, :exp, :p, :p) RETURNING lot_id
                """).param("c", customerId).param("t", transactionId).param("at", earnedAt)
                .param("exp", PointCalculator.expiresAt(earnedAt, policy.expiryMonths())).param("p", points).query(Long.class).single();
        int bal = addBalance(customerId, points);
        insert(customerId, "EARN", points, bal, lotId, transactionId, null, policy.policyId(), userId, earnedAt);
        return bal;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public int hold(long customerId, long redemptionId, int points, PointCalculator.Policy policy, Long userId) {
        Integer bal = jdbc.sql("""
                UPDATE customer.customers SET point_balance = point_balance - :p, updated_at = now()
                WHERE customer_id = :c AND point_balance >= :p RETURNING point_balance
                """).param("p", points).param("c", customerId).query(Integer.class).optional().orElse(null);
        if (bal == null) throw ApiException.conflict("INSUFFICIENT_POINTS", "Số dư điểm không đủ");
        insert(customerId, "HOLD", -points, bal, null, null, redemptionId, policy.policyId(), userId, OffsetDateTime.now());
        return bal;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public int release(long customerId, long redemptionId, int points, PointCalculator.Policy policy, Long userId) {
        int bal = addBalance(customerId, points);
        insert(customerId, "RELEASE", points, bal, null, null, redemptionId, policy.policyId(), userId, OffsetDateTime.now());
        return bal;
    }

    /** Xác nhận đổi điểm: trả lại phần đang giữ rồi trừ thật theo FIFO trên các lô điểm còn hiệu lực. */
    @Transactional(propagation = Propagation.MANDATORY)
    public int redeem(long customerId, long redemptionId, long transactionId, int points, PointCalculator.Policy policy, Long userId) {
        int bal = release(customerId, redemptionId, points, policy, userId);
        record Lot(long id, int remaining) {
        }
        List<Lot> lots = jdbc.sql("""
                SELECT lot_id, points_remaining FROM loyalty.point_lots
                WHERE customer_id = :c AND points_remaining > 0 AND expires_at > now()
                ORDER BY earned_at, lot_id FOR UPDATE
                """).param("c", customerId).query((rs, i) -> new Lot(rs.getLong(1), rs.getInt(2))).list();
        int left = points;
        OffsetDateTime now = OffsetDateTime.now();
        for (Lot lot : lots) {
            if (left == 0) break;
            int take = Math.min(left, lot.remaining());
            jdbc.sql("UPDATE loyalty.point_lots SET points_remaining = points_remaining - :t WHERE lot_id = :id")
                    .param("t", take).param("id", lot.id()).update();
            bal = addBalance(customerId, -take);
            insert(customerId, "REDEEM", -take, bal, lot.id(), transactionId, redemptionId, policy.policyId(), userId, now);
            left -= take;
        }
        if (left > 0) { // phòng hờ: điểm không gắn lô (không xảy ra trong demo)
            bal = addBalance(customerId, -left);
            insert(customerId, "REDEEM", -left, bal, null, transactionId, redemptionId, policy.policyId(), userId, now);
        }
        return bal;
    }

    public int balance(long customerId) {
        return jdbc.sql("SELECT point_balance FROM customer.customers WHERE customer_id = :c").param("c", customerId).query(Integer.class).single();
    }

    private int addBalance(long customerId, int delta) {
        return jdbc.sql("UPDATE customer.customers SET point_balance = point_balance + :d, updated_at = now() WHERE customer_id = :c RETURNING point_balance")
                .param("d", delta).param("c", customerId).query(Integer.class).single();
    }

    private void insert(long customerId, String type, int points, int balanceAfter, Long lotId, Long txnId, Long redemptionId,
                        long policyId, Long userId, OffsetDateTime at) {
        jdbc.sql("""
                INSERT INTO loyalty.point_ledgers (customer_id, entry_type, points, balance_after, lot_id, transaction_id, redemption_id,
                    policy_id, created_by, created_at)
                VALUES (:c, :type, :p, :bal, :lot, :txn, :red, :pol, :by, :at)
                """).param("c", customerId).param("type", type).param("p", points).param("bal", balanceAfter).param("lot", lotId)
                .param("txn", txnId).param("red", redemptionId).param("pol", policyId).param("by", userId).param("at", at).update();
    }
}
