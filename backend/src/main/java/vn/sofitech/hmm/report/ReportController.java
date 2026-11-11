package vn.sofitech.hmm.report;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.sofitech.hmm.admin.CurrentUser;
import vn.sofitech.hmm.customer.CustomerService;
import vn.sofitech.hmm.shared.OutboxService;
import vn.sofitech.hmm.shared.Texts;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/portal")
public class ReportController {

    private final DashboardService dashboard;
    private final Customer360Service c360;
    private final CustomerService customers;
    private final OutboxService outbox;
    private final JdbcClient jdbc;

    public ReportController(DashboardService dashboard, Customer360Service c360, CustomerService customers, OutboxService outbox, JdbcClient jdbc) {
        this.dashboard = dashboard;
        this.c360 = c360;
        this.customers = customers;
        this.outbox = outbox;
        this.jdbc = jdbc;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_MANAGER','MARKETING')")
    public Map<String, Object> dashboard(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
                                         @RequestParam(required = false) Long storeId, @AuthenticationPrincipal Jwt jwt) {
        LocalDate t = to == null ? LocalDate.now(Texts.VN) : to;
        LocalDate f = from == null ? t.minusDays(29) : from;
        return dashboard.dashboard(f, t, CurrentUser.of(jwt).scopeStore(storeId));
    }

    @GetMapping("/customers")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_MANAGER','CSKH','MARKETING')")
    public Map<String, Object> customers(@RequestParam(required = false) String q, @RequestParam(required = false) String tier,
                                         @RequestParam(required = false) String status, @RequestParam(required = false) Long storeId,
                                         @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
                                         @AuthenticationPrincipal Jwt jwt) {
        return customers.search(q, tier, status, CurrentUser.of(jwt).scopeStore(storeId), page, Math.min(size, 100));
    }

    @GetMapping("/customers/{id}/360")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_MANAGER','CSKH','MARKETING')")
    public Map<String, Object> customer360(@PathVariable long id) {
        return c360.view(id);
    }

    @GetMapping("/customers/{id}/transactions/{txnId}/lines")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_MANAGER','CSKH','MARKETING')")
    public List<Map<String, Object>> lines(@PathVariable long id, @PathVariable long txnId) {
        return c360.transactionLines(id, txnId);
    }

    @PostMapping("/customers/{id}/reveal-phone")
    @PreAuthorize("hasAnyRole('ADMIN','CSKH')")
    public Map<String, Object> reveal(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        return c360.revealPhone(id, CurrentUser.of(jwt).id());
    }

    /** Hộp thư SMS giả lập: xem các tin "đã gửi" (OTP, chào mừng, lên hạng). */
    @GetMapping("/sms-outbox")
    public List<Map<String, Object>> smsOutbox(@RequestParam(required = false) String phone) {
        String p = Texts.isBlank(phone) ? null : Texts.normalizePhone(phone);
        return outbox.latestSms(p, 100);
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> audit() {
        return jdbc.sql("""
                SELECT a.audit_id, a.created_at, u.username, a.action, a.entity_type, a.entity_id, a.after_data::text AS after_data
                FROM shared.audit_logs a LEFT JOIN admin.app_users u ON u.user_id = a.user_id ORDER BY a.audit_id DESC LIMIT 200
                """).query().listOfRows();
    }
}
