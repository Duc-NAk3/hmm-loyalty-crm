package vn.sofitech.hmm.loyalty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.sofitech.hmm.admin.CurrentUser;
import vn.sofitech.hmm.customer.CustomerService;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.Texts;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tra cứu và đăng ký thành viên tại quầy (FR-03, FR-18, FR-19); bảng hạng và xét hạng trên Portal (FR-31). */
@RestController
public class MemberController {

    private final CustomerService customers;
    private final LoyaltyConfigService config;
    private final TierService tierService;
    private final JdbcClient jdbc;

    public MemberController(CustomerService customers, LoyaltyConfigService config, TierService tierService, JdbcClient jdbc) {
        this.customers = customers;
        this.config = config;
        this.tierService = tierService;
        this.jdbc = jdbc;
    }

    /** Kết quả tra cứu: họ tên, SĐT che một phần, hạng, điểm khả dụng, điểm sắp hết hạn, trạng thái xác thực. */
    @GetMapping("/api/pos/members/lookup")
    public Map<String, Object> lookup(@RequestParam String q) {
        CustomerService.Customer c = customers.lookup(q).orElseThrow(() -> ApiException.notFound("Chưa là thành viên"));
        return summary(c);
    }

    public record RegisterRequest(@NotBlank String phone, @NotBlank String fullName, String gender, LocalDate dateOfBirth,
                                  boolean marketingConsent) {
    }

    @PostMapping("/api/pos/members")
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest req, @AuthenticationPrincipal Jwt jwt) {
        CurrentUser u = CurrentUser.of(jwt);
        return summary(customers.registerAtPos(req.phone(), req.fullName(), req.gender(), req.dateOfBirth(), req.marketingConsent(),
                u.storeId(), u.id()));
    }

    public Map<String, Object> summary(CustomerService.Customer c) {
        List<PointCalculator.Tier> tiers = config.tiers();
        PointCalculator.Tier tier = tiers.stream().filter(t -> t.code().equals(c.tierCode())).findFirst().orElseThrow();
        PointCalculator.Tier next = tiers.stream().filter(t -> t.thresholdSpend() > tier.thresholdSpend()).findFirst().orElse(null);
        Map<String, Object> expiring = jdbc.sql("""
                SELECT COALESCE(sum(points_remaining), 0) AS points, min(expires_at) AS first_expiry FROM loyalty.point_lots
                WHERE customer_id = :c AND points_remaining > 0 AND expires_at > now() AND expires_at <= now() + interval '30 days'
                """).param("c", c.customerId()).query().singleRow();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("customerId", c.customerId());
        m.put("memberCode", c.memberCode());
        m.put("fullName", c.fullName());
        m.put("phoneMasked", Texts.maskPhone(c.phone()));
        m.put("status", c.status());
        m.put("verified", "VERIFIED".equals(c.status()));
        m.put("redeemLocked", c.redeemLocked());
        m.put("tierCode", tier.code());
        m.put("tierName", tier.name());
        m.put("tierMultiplier", tier.multiplier());
        m.put("pointBalance", c.pointBalance());
        m.put("pointValueVnd", (long) c.pointBalance() * config.currentPolicy().pointValueVnd());
        m.put("expiringPoints", ((Number) expiring.get("points")).intValue());
        m.put("expiringAt", expiring.get("first_expiry"));
        m.put("spend12m", c.spend12m());
        m.put("nextTierName", next == null ? null : next.name());
        m.put("spendToNextTier", next == null ? 0 : Math.max(0, next.thresholdSpend() - c.spend12m()));
        return m;
    }

    @GetMapping("/api/portal/loyalty/tiers")
    public Map<String, Object> tiers() {
        return Map.of("tiers", config.tierTable(), "policy", config.currentPolicy());
    }

    @PostMapping("/api/portal/loyalty/tier-review")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> runTierReview() {
        Map<String, Object> r = new LinkedHashMap<>(tierService.dailyReview());
        r.put("ranAt", OffsetDateTime.now(Texts.VN));
        return r;
    }
}
