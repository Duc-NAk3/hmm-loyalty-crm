package vn.sofitech.hmm.pos.service;

import vn.sofitech.hmm.pos.dto.shift.ShiftView;
import vn.sofitech.hmm.pos.dto.shift.CloseShiftResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sofitech.hmm.admin.AdminFacade;
import vn.sofitech.hmm.admin.AdminFacade.TerminalInfo;
import vn.sofitech.hmm.admin.CurrentUser;
import vn.sofitech.hmm.pos.repository.SalesTransactionRepository;
import vn.sofitech.hmm.pos.domain.Shift;
import vn.sofitech.hmm.pos.repository.ShiftRepository;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.Texts;

import java.time.OffsetDateTime;
import java.util.Optional;

/** Ca bán hàng (FR-01; đóng ca đơn giản – đối chiếu chi tiết FR-08 không thuộc bản demo). */
@Service
public class ShiftService {

    private final ShiftRepository shifts;
    private final SalesTransactionRepository transactions;
    private final AdminFacade admin;

    public ShiftService(ShiftRepository shifts, SalesTransactionRepository transactions, AdminFacade admin) {
        this.shifts = shifts;
        this.transactions = transactions;
        this.admin = admin;
    }

    /** Quầy phải đang hoạt động và thuộc cửa hàng của người dùng. */
    public TerminalInfo checkTerminal(CurrentUser u, long terminalId) {
        TerminalInfo t = admin.findActiveTerminal(terminalId).orElseThrow(() -> ApiException.badRequest("BAD_TERMINAL", "Quầy không hợp lệ"));
        if (u.storeId() == null || !u.storeId().equals(t.storeId())) throw ApiException.forbidden("Quầy không thuộc cửa hàng của bạn");
        return t;
    }

    @Transactional(readOnly = true)
    public Optional<ShiftView> current(CurrentUser u, long terminalId) {
        checkTerminal(u, terminalId);
        return shifts.findByTerminalIdAndClosedAtIsNull(terminalId).map(this::toView);
    }

    /** Ca đang mở của quầy (dùng khi hoàn tất hóa đơn). */
    public Optional<Shift> openShift(long terminalId) {
        return shifts.findByTerminalIdAndClosedAtIsNull(terminalId);
    }

    @Transactional
    public ShiftView open(CurrentUser u, long terminalId, long openingCash) {
        checkTerminal(u, terminalId);
        if (shifts.findByTerminalIdAndClosedAtIsNull(terminalId).isPresent())
            throw ApiException.conflict("SHIFT_OPEN", "Quầy này đang có ca mở, cần đóng ca trước");
        // Ràng buộc CSDL ux_shifts_one_open vẫn chặn trường hợp hai máy mở ca cùng lúc
        return toView(shifts.saveAndFlush(new Shift(terminalId, u.id(), OffsetDateTime.now(Texts.VN), openingCash)));
    }

    @Transactional
    public CloseShiftResult close(CurrentUser u, long shiftId, long countedCash) {
        Shift s = shifts.findForUpdate(shiftId).orElseThrow(() -> ApiException.notFound("Không tìm thấy ca"));
        checkTerminal(u, s.getTerminalId());
        s.close(OffsetDateTime.now(Texts.VN), transactions.sumCashByShift(shiftId), countedCash, null);
        return new CloseShiftResult(s.getId(), s.getExpectedCash(), s.getCountedCash(), s.getCashDifference());
    }

    private ShiftView toView(Shift s) {
        String cashier = admin.findUser(s.getCashierId()).map(AdminFacade.UserInfo::fullName).orElse(null);
        return new ShiftView(s.getId(), s.getTerminalId(), s.getCashierId(), cashier, s.getOpenedAt(), s.getOpeningCash(),
                transactions.countByShiftId(s.getId()), transactions.sumCashByShift(s.getId()));
    }
}
