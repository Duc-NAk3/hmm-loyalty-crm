package vn.sofitech.hmm.pos.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.sofitech.hmm.shared.ApiException;

import java.time.OffsetDateTime;

/** Ca bán hàng của thu ngân tại một quầy (pos.shifts). Mỗi quầy chỉ có một ca đang mở (FR-01). */
@Entity
@Table(schema = "pos", name = "shifts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "shift_seq")
    @SequenceGenerator(name = "shift_seq", schema = "pos", sequenceName = "shifts_shift_id_seq", allocationSize = 50)
    @Column(name = "shift_id")
    private Long id;

    @Column(name = "terminal_id", nullable = false)
    private Long terminalId;

    @Column(name = "cashier_id", nullable = false)
    private Long cashierId;

    @Column(name = "opened_at", nullable = false)
    private OffsetDateTime openedAt;

    @Column(name = "opening_cash", nullable = false)
    private long openingCash;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "expected_cash")
    private Long expectedCash;

    @Column(name = "counted_cash")
    private Long countedCash;

    @Column(name = "cash_difference")
    private Long cashDifference;

    @Column(name = "confirmed_by")
    private Long confirmedBy;

    public Shift(Long terminalId, Long cashierId, OffsetDateTime openedAt, long openingCash) {
        if (openingCash < 0) throw ApiException.badRequest("CASH", "Tiền đầu ca không hợp lệ");
        this.terminalId = terminalId;
        this.cashierId = cashierId;
        this.openedAt = openedAt;
        this.openingCash = openingCash;
    }

    /** Đóng ca: hệ thống tính tiền mặt kỳ vọng = tiền đầu ca + tiền mặt bán được. */
    public void close(OffsetDateTime at, long cashSales, long countedCash, Long confirmedBy) {
        if (closedAt != null) throw ApiException.conflict("SHIFT_CLOSED", "Ca đã đóng");
        this.closedAt = at;
        this.expectedCash = openingCash + cashSales;
        this.countedCash = countedCash;
        this.cashDifference = countedCash - expectedCash;
        this.confirmedBy = confirmedBy;
    }

    public boolean isOpen() {
        return closedAt == null;
    }
}
