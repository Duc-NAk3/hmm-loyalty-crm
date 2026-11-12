package vn.sofitech.hmm.loyalty.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.sofitech.hmm.customer.CustomerFacade;
import vn.sofitech.hmm.loyalty.PointCalculator;
import vn.sofitech.hmm.loyalty.domain.*;
import vn.sofitech.hmm.loyalty.domain.PointLedger.EntryType;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.Texts;

import java.time.OffsetDateTime;

/**
 * Sổ cái điểm (FR-33): mọi biến động là một bản ghi bất biến; customers.point_balance luôn bằng tổng sổ cái.
 * <ul>
 *   <li>EARN: +điểm, tạo một lô điểm (point_lots) để trừ và hết hạn theo FIFO.</li>
 *   <li>HOLD: −điểm khi OTP đúng (điểm bị giữ không còn khả dụng).</li>
 *   <li>RELEASE: +điểm trả lại khi hủy, quá hạn, hoặc ngay trước khi ghi REDEEM.</li>
 *   <li>REDEEM: −điểm theo từng lô FIFO khi hóa đơn hoàn tất.</li>
 * </ul>
 * Các hàm phải chạy trong giao dịch của nghiệp vụ gọi tới (MANDATORY).
 */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class LedgerService {

    private final PointLotRepository lots;
    private final PointLedgerRepository ledger;
    private final LoyaltyPolicyRepository policies;
    private final CustomerFacade customers;

    public LedgerService(PointLotRepository lots, PointLedgerRepository ledger, LoyaltyPolicyRepository policies, CustomerFacade customers) {
        this.lots = lots;
        this.ledger = ledger;
        this.policies = policies;
        this.customers = customers;
    }

    public int earn(long customerId, long transactionId, int points, OffsetDateTime earnedAt, PointCalculator.Policy policy, Long userId) {
        if (points <= 0) return customers.findById(customerId).orElseThrow().pointBalance();
        PointLot lot = lots.save(new PointLot(customerId, transactionId, earnedAt, PointCalculator.expiresAt(earnedAt, policy.expiryMonths()), points));
        int balance = customers.adjustPointBalance(customerId, points, false);
        write(customerId, EntryType.EARN, points, balance, lot, transactionId, null, policy, userId, earnedAt);
        return balance;
    }

    public int hold(long customerId, PointRedemption redemption, int points, PointCalculator.Policy policy, Long userId) {
        int balance = customers.adjustPointBalance(customerId, -points, false); // báo lỗi nếu số dư không đủ
        write(customerId, EntryType.HOLD, -points, balance, null, null, redemption, policy, userId, now());
        return balance;
    }

    public int release(long customerId, PointRedemption redemption, int points, PointCalculator.Policy policy, Long userId) {
        int balance = customers.adjustPointBalance(customerId, points, false);
        write(customerId, EntryType.RELEASE, points, balance, null, null, redemption, policy, userId, now());
        return balance;
    }

    /** Xác nhận đổi điểm: trả lại phần đang giữ rồi trừ thật theo FIFO trên các lô còn hiệu lực. */
    public int redeem(long customerId, PointRedemption redemption, long transactionId, int points, PointCalculator.Policy policy, Long userId) {
        int balance = release(customerId, redemption, points, policy, userId);
        int left = points;
        OffsetDateTime now = now();
        for (PointLot lot : lots.findOpenLotsForUpdate(customerId, now)) {
            if (left == 0) break;
            int take = lot.consume(left);
            balance = customers.adjustPointBalance(customerId, -take, false);
            write(customerId, EntryType.REDEEM, -take, balance, lot, transactionId, redemption, policy, userId, now);
            left -= take;
        }
        if (left > 0) throw ApiException.conflict("INSUFFICIENT_LOTS", "Không đủ điểm còn hiệu lực để đổi");
        return balance;
    }

    private void write(long customerId, EntryType type, int points, int balanceAfter, PointLot lot, Long transactionId,
                       PointRedemption redemption, PointCalculator.Policy policy, Long userId, OffsetDateTime at) {
        ledger.save(new PointLedger(customerId, type, points, balanceAfter, lot, transactionId, redemption,
                policies.getReferenceById(policy.policyId()), userId, at));
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(Texts.VN);
    }
}
