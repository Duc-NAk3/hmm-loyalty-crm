package vn.sofitech.hmm.pos;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.sofitech.hmm.admin.CurrentUser;
import vn.sofitech.hmm.customer.CustomerService;
import vn.sofitech.hmm.loyalty.PointCalculator;
import vn.sofitech.hmm.loyalty.RedemptionService;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.OutboxService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** API cho màn hình thu ngân: ca, hóa đơn, đổi điểm có OTP (FR-01, FR-02, FR-04, FR-30). */
@RestController
@RequestMapping("/api/pos")
public class PosController {

    private final ShiftService shifts;
    private final SaleService sales;
    private final RedemptionService redemptions;
    private final CustomerService customers;
    private final OutboxService outbox;

    public PosController(ShiftService shifts, SaleService sales, RedemptionService redemptions, CustomerService customers, OutboxService outbox) {
        this.shifts = shifts;
        this.sales = sales;
        this.redemptions = redemptions;
        this.customers = customers;
        this.outbox = outbox;
    }

    // ---------------- ca ----------------

    @GetMapping("/shifts/current")
    public Map<String, Object> currentShift(@RequestParam long terminalId, @AuthenticationPrincipal Jwt jwt) {
        shifts.checkTerminal(CurrentUser.of(jwt), terminalId);
        return shifts.current(terminalId).orElse(Map.of());
    }

    public record OpenShift(long terminalId, long openingCash) {
    }

    @PostMapping("/shifts/open")
    public Map<String, Object> open(@RequestBody OpenShift req, @AuthenticationPrincipal Jwt jwt) {
        return shifts.open(CurrentUser.of(jwt), req.terminalId(), req.openingCash());
    }

    public record CloseShift(long countedCash) {
    }

    @PostMapping("/shifts/{id}/close")
    public Map<String, Object> close(@PathVariable long id, @RequestBody CloseShift req, @AuthenticationPrincipal Jwt jwt) {
        return shifts.close(CurrentUser.of(jwt), id, req.countedCash());
    }

    // ---------------- hóa đơn ----------------

    @PostMapping("/sales")
    public Map<String, Object> checkout(@RequestBody SaleService.SaleRequest req, @AuthenticationPrincipal Jwt jwt) {
        return sales.checkout(CurrentUser.of(jwt), req);
    }

    // ---------------- đổi điểm ----------------

    public record QuoteRequest(long customerId, List<SaleService.LineInput> lines) {
    }

    @PostMapping("/redemptions/quote")
    public Map<String, Object> quote(@RequestBody QuoteRequest req, @AuthenticationPrincipal Jwt jwt) {
        long redeemable = SaleService.redeemableAmount(sales.price(req.lines(), CurrentUser.of(jwt).storeId()));
        PointCalculator.RedeemQuote q = redemptions.quote(req.customerId(), redeemable);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("allowed", q.allowed());
        m.put("maxPoints", q.maxPoints());
        m.put("maxAmount", q.maxAmount());
        m.put("reason", q.reason());
        m.put("redeemableAmount", redeemable);
        return m;
    }

    public record RedeemRequest(long customerId, long terminalId, int points, List<SaleService.LineInput> lines) {
    }

    @PostMapping("/redemptions")
    public Map<String, Object> request(@RequestBody RedeemRequest req, @AuthenticationPrincipal Jwt jwt) {
        CurrentUser u = CurrentUser.of(jwt);
        shifts.checkTerminal(u, req.terminalId());
        long redeemable = SaleService.redeemableAmount(sales.price(req.lines(), u.storeId()));
        return redemptions.request(req.customerId(), req.terminalId(), req.points(), redeemable);
    }

    public record VerifyRequest(long terminalId, String otp) {
    }

    @PostMapping("/redemptions/{id}/verify")
    public Map<String, Object> verify(@PathVariable long id, @RequestBody VerifyRequest req, @AuthenticationPrincipal Jwt jwt) {
        CurrentUser u = CurrentUser.of(jwt);
        shifts.checkTerminal(u, req.terminalId());
        return redemptions.verifyOtp(id, req.terminalId(), req.otp(), u.id());
    }

    public record CancelRequest(long terminalId) {
    }

    @PostMapping("/redemptions/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable long id, @RequestBody CancelRequest req, @AuthenticationPrincipal Jwt jwt) {
        CurrentUser u = CurrentUser.of(jwt);
        shifts.checkTerminal(u, req.terminalId());
        return redemptions.cancel(id, req.terminalId(), u.id());
    }

    /** Công cụ demo: "điện thoại của khách" – xem SMS giả lập gửi tới thành viên (OTP, chào mừng). */
    @GetMapping("/mock-phone")
    public List<Map<String, Object>> mockPhone(@RequestParam long customerId) {
        CustomerService.Customer c = customers.findById(customerId).orElseThrow(() -> ApiException.notFound("Không tìm thấy thành viên"));
        return outbox.latestSms(c.phone(), 5);
    }
}
