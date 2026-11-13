package vn.sofitech.hmm.loyalty.controller;

import vn.sofitech.hmm.loyalty.dto.member.MemberSummaryDto;
import vn.sofitech.hmm.loyalty.dto.member.RegisterMemberRequestDto;
import vn.sofitech.hmm.loyalty.dto.tier.TiersResponseDto;
import vn.sofitech.hmm.loyalty.dto.tier.TierReviewResponseDto;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.sofitech.hmm.admin.CurrentUser;
import vn.sofitech.hmm.customer.CustomerFacade;
import vn.sofitech.hmm.loyalty.service.LoyaltyConfigService;
import vn.sofitech.hmm.loyalty.service.MemberService;
import vn.sofitech.hmm.loyalty.service.TierService;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.Texts;

import java.time.OffsetDateTime;

/** Tra cứu và đăng ký thành viên tại quầy (FR-03, FR-18, FR-19); bảng hạng và xét hạng trên Portal (FR-31). */
@RestController
public class MemberController {

    private final CustomerFacade customers;
    private final MemberService members;
    private final LoyaltyConfigService config;
    private final TierService tierService;

    public MemberController(CustomerFacade customers, MemberService members, LoyaltyConfigService config, TierService tierService) {
        this.customers = customers;
        this.members = members;
        this.config = config;
        this.tierService = tierService;
    }

    @GetMapping("/api/pos/members/lookup")
    public MemberSummaryDto lookup(@RequestParam String q) {
        return members.summary(customers.lookup(q).orElseThrow(() -> ApiException.notFound("Chưa là thành viên")));
    }

    @PostMapping("/api/pos/members")
    public MemberSummaryDto register(@Valid @RequestBody RegisterMemberRequestDto req, @AuthenticationPrincipal Jwt jwt) {
        CurrentUser u = CurrentUser.of(jwt);
        return members.summary(customers.registerAtPos(req.getPhone(), req.getFullName(), req.getGender(), req.getDateOfBirth(),
                req.consentsToMarketing(), u.storeId(), u.id()));
    }

    @GetMapping("/api/portal/loyalty/tiers")
    public TiersResponseDto tiers() {
        return new TiersResponseDto(members.tierTable(), config.currentPolicy());
    }

    @PostMapping("/api/portal/loyalty/tier-review")
    @PreAuthorize("hasRole('ADMIN')")
    public TierReviewResponseDto runTierReview() {
        TierService.ReviewResult r = tierService.dailyReview();
        return new TierReviewResponseDto(r.customersChecked(), r.spendRecalculated(), r.upgraded(), r.downgraded(), OffsetDateTime.now(Texts.VN));
    }
}
