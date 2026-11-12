package vn.sofitech.hmm.report.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.sofitech.hmm.admin.CurrentUser;
import vn.sofitech.hmm.report.service.CustomerQueryService;
import vn.sofitech.hmm.report.service.DashboardService;
import vn.sofitech.hmm.report.web.ReportDtos.*;
import vn.sofitech.hmm.shared.PageResult;
import vn.sofitech.hmm.shared.SmsOutbox;
import vn.sofitech.hmm.shared.Texts;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/portal")
public class ReportController {

    private final DashboardService dashboard;
    private final CustomerQueryService customers;
    private final SmsOutbox sms;

    public ReportController(DashboardService dashboard, CustomerQueryService customers, SmsOutbox sms) {
        this.dashboard = dashboard;
        this.customers = customers;
        this.sms = sms;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_MANAGER','MARKETING')")
    public DashboardView dashboard(@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
                                   @RequestParam(required = false) Long storeId, @AuthenticationPrincipal Jwt jwt) {
        LocalDate t = to == null ? LocalDate.now(Texts.VN) : to;
        LocalDate f = from == null ? t.minusDays(29) : from;
        return dashboard.dashboard(f, t, CurrentUser.of(jwt).scopeStore(storeId));
    }

    @GetMapping("/customers")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_MANAGER','CSKH','MARKETING')")
    public PageResult<CustomerRow> customers(@RequestParam(required = false) String q, @RequestParam(required = false) String tier,
                                             @RequestParam(required = false) String status, @RequestParam(required = false) Long storeId,
                                             @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
                                             @AuthenticationPrincipal Jwt jwt) {
        return customers.search(q, tier, status, CurrentUser.of(jwt).scopeStore(storeId), page, Math.min(size, 100));
    }

    @GetMapping("/customers/{id}/360")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_MANAGER','CSKH','MARKETING')")
    public Customer360View customer360(@PathVariable long id) {
        return customers.view(id);
    }

    @GetMapping("/customers/{id}/transactions/{txnId}/lines")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_MANAGER','CSKH','MARKETING')")
    public List<LineRow> lines(@PathVariable long id, @PathVariable long txnId) {
        return customers.transactionLines(id, txnId);
    }

    @PostMapping("/customers/{id}/reveal-phone")
    @PreAuthorize("hasAnyRole('ADMIN','CSKH')")
    public PhoneView reveal(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        return customers.revealPhone(id, CurrentUser.of(jwt).id());
    }

    /** Hộp thư SMS giả lập: xem các tin "đã gửi" (OTP, chào mừng, lên hạng). */
    @GetMapping("/sms-outbox")
    public List<SmsOutbox.SmsView> smsOutbox(@RequestParam(required = false) String phone) {
        return sms.latest(Texts.isBlank(phone) ? null : Texts.normalizePhone(phone), 100);
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuditRow> audit() {
        return customers.auditLogs();
    }
}
