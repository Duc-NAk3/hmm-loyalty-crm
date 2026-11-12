package vn.sofitech.hmm.pos.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.sofitech.hmm.admin.CurrentUser;
import vn.sofitech.hmm.customer.CustomerFacade;
import vn.sofitech.hmm.loyalty.LoyaltyFacade;
import vn.sofitech.hmm.loyalty.LoyaltyFacade.RedemptionHeld;
import vn.sofitech.hmm.loyalty.LoyaltyFacade.RedemptionInfo;
import vn.sofitech.hmm.loyalty.LoyaltyFacade.RedemptionRequested;
import vn.sofitech.hmm.loyalty.PointCalculator;
import vn.sofitech.hmm.pos.service.SaleService;
import vn.sofitech.hmm.pos.service.ShiftService;
import vn.sofitech.hmm.pos.web.PosDtos.*;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.SmsOutbox;

import java.util.List;

/** API cho màn hình thu ngân: ca, hóa đơn, đổi điểm có OTP (FR-01, FR-02, FR-04, FR-30). */
@RestController
@RequestMapping("/api/pos")
public class PosController {

    private final ShiftService shifts;
    private final SaleService sales;
    private final LoyaltyFacade loyalty;
    private final CustomerFacade customers;
    private final SmsOutbox sms;

    public PosController(ShiftService shifts, SaleService sales, LoyaltyFacade loyalty, CustomerFacade customers, SmsOutbox sms) {
        this.shifts = shifts;
        this.sales = sales;
        this.loyalty = loyalty;
        this.customers = customers;
        this.sms = sms;
    }

    // ---------------- ca ----------------

    /** Ca đang mở của quầy; 204 nếu chưa có ca. */
    @GetMapping("/shifts/current")
    public ResponseEntity<ShiftView> currentShift(@RequestParam long terminalId, @AuthenticationPrincipal Jwt jwt) {
        return shifts.current(CurrentUser.of(jwt), terminalId).map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/shifts/open")
    public ShiftView open(@RequestBody OpenShiftRequest req, @AuthenticationPrincipal Jwt jwt) {
        return shifts.open(CurrentUser.of(jwt), req.terminalId(), req.openingCash());
    }

    @PostMapping("/shifts/{id}/close")
    public CloseShiftResult close(@PathVariable long id, @RequestBody CloseShiftRequest req, @AuthenticationPrincipal Jwt jwt) {
        return shifts.close(CurrentUser.of(jwt), id, req.countedCash());
    }

    // ---------------- hóa đơn ----------------

    @PostMapping("/sales")
    public ReceiptView checkout(@RequestBody SaleRequest req, @AuthenticationPrincipal Jwt jwt) {
        return sales.checkout(CurrentUser.of(jwt), req);
    }

    // ---------------- đổi điểm ----------------

    @PostMapping("/redemptions/quote")
    public QuoteResponse quote(@RequestBody QuoteRequest req, @AuthenticationPrincipal Jwt jwt) {
        long redeemable = SaleService.redeemableAmount(sales.price(req.lines(), CurrentUser.of(jwt).storeId()));
        PointCalculator.RedeemQuote q = loyalty.quote(req.customerId(), redeemable);
        return new QuoteResponse(q.allowed(), q.maxPoints(), q.maxAmount(), q.reason(), redeemable);
    }

    @PostMapping("/redemptions")
    public RedemptionRequested request(@RequestBody RedeemRequest req, @AuthenticationPrincipal Jwt jwt) {
        CurrentUser u = CurrentUser.of(jwt);
        shifts.checkTerminal(u, req.terminalId());
        long redeemable = SaleService.redeemableAmount(sales.price(req.lines(), u.storeId()));
        return loyalty.requestRedemption(req.customerId(), req.terminalId(), req.points(), redeemable);
    }

    @PostMapping("/redemptions/{id}/verify")
    public RedemptionHeld verify(@PathVariable long id, @RequestBody VerifyRequest req, @AuthenticationPrincipal Jwt jwt) {
        CurrentUser u = CurrentUser.of(jwt);
        shifts.checkTerminal(u, req.terminalId());
        return loyalty.verifyOtp(id, req.terminalId(), req.otp(), u.id());
    }

    @PostMapping("/redemptions/{id}/cancel")
    public RedemptionInfo cancel(@PathVariable long id, @RequestBody CancelRequest req, @AuthenticationPrincipal Jwt jwt) {
        CurrentUser u = CurrentUser.of(jwt);
        shifts.checkTerminal(u, req.terminalId());
        return loyalty.cancelRedemption(id, req.terminalId(), u.id());
    }

    /** Công cụ demo: "điện thoại của khách" – xem SMS giả lập gửi tới thành viên (OTP, chào mừng). */
    @GetMapping("/mock-phone")
    public List<SmsOutbox.SmsView> mockPhone(@RequestParam long customerId) {
        CustomerFacade.CustomerInfo c = customers.findById(customerId).orElseThrow(() -> ApiException.notFound("Không tìm thấy thành viên"));
        return sms.latest(c.phone(), 5);
    }
}
