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

    /** Đăng ký nhanh tại quầy (FR-19). {@code marketingConsent} không gửi thì coi như không đồng ý. */
    public record RegisterRequest(@NotBlank(message = "SĐT là bắt buộc") String phone,
                                  @NotBlank(message = "Họ tên là bắt buộc") String fullName, String gender, LocalDate dateOfBirth,
                                  Boolean marketingConsent) {
        public boolean consentsToMarketing() {
            return Boolean.TRUE.equals(marketingConsent);
        }
    }

    public record TierView(String tierCode, String name, long thresholdSpend, BigDecimal multiplier, String benefits, long members) {
    }

    public record TiersResponse(List<TierView> tiers, PointCalculator.Policy policy) {
    }

    public record TierReviewResponse(int customersChecked, int spendRecalculated, int upgraded, int downgraded, OffsetDateTime ranAt) {
    }
}
