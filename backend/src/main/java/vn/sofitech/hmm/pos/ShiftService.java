package vn.sofitech.hmm.pos;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sofitech.hmm.admin.CurrentUser;
import vn.sofitech.hmm.shared.ApiException;

import java.util.Map;
import java.util.Optional;

/** Ca bán hàng (FR-01; đóng ca đơn giản – đối chiếu chi tiết FR-08 không thuộc bản demo). */
@Service
public class ShiftService {

    private final JdbcClient jdbc;

    public ShiftService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public long terminalStore(long terminalId) {
        return jdbc.sql("SELECT store_id FROM admin.pos_terminals WHERE terminal_id = :t AND status = 'ACTIVE'").param("t", terminalId)
                .query(Long.class).optional().orElseThrow(() -> ApiException.badRequest("BAD_TERMINAL", "Quầy không hợp lệ"));
    }

    public void checkTerminal(CurrentUser u, long terminalId) {
        if (u.storeId() == null || terminalStore(terminalId) != u.storeId())
            throw ApiException.forbidden("Quầy không thuộc cửa hàng của bạn");
    }

    public Optional<Map<String, Object>> current(long terminalId) {
        return jdbc.sql("""
                SELECT s.shift_id, s.terminal_id, s.cashier_id, u.full_name AS cashier_name, s.opened_at, s.opening_cash,
                       (SELECT count(*) FROM pos.sales_transactions t WHERE t.shift_id = s.shift_id) AS receipt_count,
                       (SELECT COALESCE(sum(p.amount), 0) FROM pos.payments p JOIN pos.sales_transactions t ON t.transaction_id = p.transaction_id
                         WHERE t.shift_id = s.shift_id AND p.method = 'CASH') AS cash_sales
                FROM pos.shifts s JOIN admin.app_users u ON u.user_id = s.cashier_id
                WHERE s.terminal_id = :t AND s.closed_at IS NULL
                """).param("t", terminalId).query().listOfRows().stream().findFirst();
    }

    @Transactional
    public Map<String, Object> open(CurrentUser u, long terminalId, long openingCash) {
        checkTerminal(u, terminalId);
        if (openingCash < 0) throw ApiException.badRequest("CASH", "Tiền đầu ca không hợp lệ");
        try {
            jdbc.sql("INSERT INTO pos.shifts (terminal_id, cashier_id, opened_at, opening_cash) VALUES (:t, :c, now(), :cash)")
                    .param("t", terminalId).param("c", u.id()).param("cash", openingCash).update();
        } catch (DuplicateKeyException e) {
            throw ApiException.conflict("SHIFT_OPEN", "Quầy này đang có ca mở, cần đóng ca trước");
        }
        return current(terminalId).orElseThrow();
    }

    @Transactional
    public Map<String, Object> close(CurrentUser u, long shiftId, long countedCash) {
        Map<String, Object> s = jdbc.sql("SELECT terminal_id, cashier_id, opening_cash, closed_at FROM pos.shifts WHERE shift_id = :id FOR UPDATE")
                .param("id", shiftId).query().listOfRows().stream().findFirst().orElseThrow(() -> ApiException.notFound("Không tìm thấy ca"));
        if (s.get("closed_at") != null) throw ApiException.conflict("SHIFT_CLOSED", "Ca đã đóng");
        checkTerminal(u, ((Number) s.get("terminal_id")).longValue());
        long cashSales = jdbc.sql("""
                SELECT COALESCE(sum(p.amount), 0) FROM pos.payments p JOIN pos.sales_transactions t ON t.transaction_id = p.transaction_id
                WHERE t.shift_id = :id AND p.method = 'CASH'
                """).param("id", shiftId).query(Long.class).single();
        long expected = ((Number) s.get("opening_cash")).longValue() + cashSales;
        jdbc.sql("""
                UPDATE pos.shifts SET closed_at = now(), expected_cash = :e, counted_cash = :c, cash_difference = :d WHERE shift_id = :id
                """).param("e", expected).param("c", countedCash).param("d", countedCash - expected).param("id", shiftId).update();
        return Map.of("shiftId", shiftId, "expectedCash", expected, "countedCash", countedCash, "difference", countedCash - expected);
    }
}
