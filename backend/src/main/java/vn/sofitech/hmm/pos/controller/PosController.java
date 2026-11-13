package vn.sofitech.hmm.pos.controller;

import vn.sofitech.hmm.customer.api.CustomerInfo;
import vn.sofitech.hmm.pos.dto.shift.ShiftDto;
import vn.sofitech.hmm.pos.dto.shift.OpenShiftRequestDto;
import vn.sofitech.hmm.pos.dto.shift.CloseShiftRequestDto;
import vn.sofitech.hmm.pos.dto.shift.CloseShiftResultDto;
import vn.sofitech.hmm.pos.dto.sale.SaleRequestDto;
import vn.sofitech.hmm.pos.dto.sale.ReceiptDto;
import vn.sofitech.hmm.pos.dto.redemption.QuoteRequestDto;
import vn.sofitech.hmm.pos.dto.redemption.QuoteResponseDto;
import vn.sofitech.hmm.pos.dto.redemption.RedeemRequestDto;
import vn.sofitech.hmm.pos.dto.redemption.VerifyRequestDto;
import vn.sofitech.hmm.pos.dto.redemption.CancelRequestDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.sofitech.hmm.admin.CurrentUser;
import vn.sofitech.hmm.customer.CustomerFacade;
import vn.sofitech.hmm.loyalty.LoyaltyFacade;
import vn.sofitech.hmm.loyalty.api.RedemptionHeld;
import vn.sofitech.hmm.loyalty.api.RedemptionInfo;
import vn.sofitech.hmm.loyalty.api.RedemptionRequested;
import vn.sofitech.hmm.loyalty.PointCalculator;
import vn.sofitech.hmm.pos.service.SaleService;
import vn.sofitech.hmm.pos.service.ShiftService;
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
    public ResponseEntity<ShiftDto> currentShift(@RequestParam long terminalId, @AuthenticationPrincipal Jwt jwt) {
        return shifts.current(CurrentUser.of(jwt), terminalId).map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/shifts/open")
    public ShiftDto open(@RequestBody OpenShiftRequestDto req, @AuthenticationPrincipal Jwt jwt) {
        return shifts.open(CurrentUser.of(jwt), req.getTerminalId(), req.getOpeningCash());
    }

    @PostMapping("/shifts/{id}/close")
    public CloseShiftResultDto close(@PathVariable long id, @RequestBody CloseShiftRequestDto req, @AuthenticationPrincipal Jwt jwt) {
        return shifts.close(CurrentUser.of(jwt), id, req.getCountedCash());
    }

    // ---------------- hóa đơn ----------------

    @PostMapping("/sales")
    public ReceiptDto checkout(@RequestBody SaleRequestDto req, @AuthenticationPrincipal Jwt jwt) {
        return sales.checkout(CurrentUser.of(jwt), req);
    }

    // ---------------- đổi điểm ----------------

    @PostMapping("/redemptions/quote")
    public QuoteResponseDto quote(@RequestBody QuoteRequestDto req, @AuthenticationPrincipal Jwt jwt) {
        long redeemable = SaleService.redeemableAmount(sales.price(req.getLines(), CurrentUser.of(jwt).storeId()));
        PointCalculator.RedeemQuote q = loyalty.quote(req.getCustomerId(), redeemable);
        return new QuoteResponseDto(q.allowed(), q.maxPoints(), q.maxAmount(), q.reason(), redeemable);
    }

    @PostMapping("/redemptions")
    public RedemptionRequested request(@RequestBody RedeemRequestDto req, @AuthenticationPrincipal Jwt jwt) {
        CurrentUser u = CurrentUser.of(jwt);
        shifts.checkTerminal(u, req.getTerminalId());
        long redeemable = SaleService.redeemableAmount(sales.price(req.getLines(), u.storeId()));
        return loyalty.requestRedemption(req.getCustomerId(), req.getTerminalId(), req.getPoints(), redeemable);
    }

    @PostMapping("/redemptions/{id}/verify")
    public RedemptionHeld verify(@PathVariable long id, @RequestBody VerifyRequestDto req, @AuthenticationPrincipal Jwt jwt) {
        CurrentUser u = CurrentUser.of(jwt);
        shifts.checkTerminal(u, req.getTerminalId());
        return loyalty.verifyOtp(id, req.getTerminalId(), req.getOtp(), u.id());
    }

    @PostMapping("/redemptions/{id}/cancel")
    public RedemptionInfo cancel(@PathVariable long id, @RequestBody CancelRequestDto req, @AuthenticationPrincipal Jwt jwt) {
        CurrentUser u = CurrentUser.of(jwt);
        shifts.checkTerminal(u, req.getTerminalId());
        return loyalty.cancelRedemption(id, req.getTerminalId(), u.id());
    }

    /** Công cụ demo: "điện thoại của khách" – xem SMS giả lập gửi tới thành viên (OTP, chào mừng). */
    @GetMapping("/mock-phone")
    public List<SmsOutbox.SmsView> mockPhone(@RequestParam long customerId) {
        CustomerInfo c = customers.findById(customerId).orElseThrow(() -> ApiException.notFound("Không tìm thấy thành viên"));
        return sms.latest(c.getPhone(), 5);
    }
}
