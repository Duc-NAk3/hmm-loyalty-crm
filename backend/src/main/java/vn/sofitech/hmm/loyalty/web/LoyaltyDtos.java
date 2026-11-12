package vn.sofitech.hmm.loyalty.web;

import jakarta.validation.constraints.NotBlank;
import vn.sofitech.hmm.loyalty.PointCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** DTO của module loyalty. */
public final class LoyaltyDtos {

    private LoyaltyDtos() {
    }

    /** Kết quả tra cứu thành viên tại quầy (FR-18): SĐT che một phần, hạng, điểm khả dụng, điểm sắp hết hạn. */
    public record MemberSummary(Long customerId, String memberCode, String fullName, String phoneMasked, String status,
                                boolean verified, boolean redeemLocked, String tierCode, String tierName, BigDecimal tierMultiplier,
                                int pointBalance, long pointValueVnd, long expiringPoints, OffsetDateTime expiringAt, long spend12m,
                                String nextTierName, long spendToNextTier) {
    }

    public record RegisterRequest(@NotBlank String phone, @NotBlank String fullName, String gender, LocalDate dateOfBirth,
                                  boolean marketingConsent) {
    }

    public record TierView(String tierCode, String name, long thresholdSpend, BigDecimal multiplier, String benefits, long members) {
    }

    public record TiersResponse(List<TierView> tiers, PointCalculator.Policy policy) {
    }

    public record TierReviewResponse(int customersChecked, int spendRecalculated, int upgraded, int downgraded, OffsetDateTime ranAt) {
    }
}
