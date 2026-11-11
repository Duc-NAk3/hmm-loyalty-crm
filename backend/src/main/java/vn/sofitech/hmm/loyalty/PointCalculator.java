package vn.sofitech.hmm.loyalty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

import static vn.sofitech.hmm.shared.Texts.VN;

/** Công thức điểm theo BA mục 7 (BRL-03, BRL-04, BRL-08, BRL-09, BRL-10, BRL-11) – hàm thuần, không truy cập CSDL. */
public final class PointCalculator {

    public record Policy(long policyId, int earnUnitVnd, int pointValueVnd, int minRedeemPoints, int maxRedeemPct,
                         int expiryMonths, int holdMinutes) {
    }

    public record Tier(String code, String name, long thresholdSpend, BigDecimal multiplier) {
    }

    public record RedeemQuote(boolean allowed, int maxPoints, long maxAmount, String reason) {
    }

    private PointCalculator() {
    }

    /** BRL-03 + BRL-04: điểm = floor(floor(giá trị tính điểm / 10.000) × hệ số hạng × hệ số chương trình). */
    public static int earnedPoints(long eligibleAmount, Policy p, BigDecimal tierMultiplier, BigDecimal promoMultiplier) {
        if (eligibleAmount <= 0) return 0;
        long base = eligibleAmount / p.earnUnitVnd();
        return BigDecimal.valueOf(base).multiply(tierMultiplier).multiply(promoMultiplier)
                .setScale(0, RoundingMode.FLOOR).intValue();
    }

    /**
     * BRL-08 + BRL-09: số điểm tối đa được dùng = min(số dư, 50% giá trị các mặt hàng được phép / 100đ);
     * phải đạt mức tối thiểu 50 điểm.
     */
    public static RedeemQuote quote(long redeemableLineAmount, int balance, Policy p) {
        long maxAmountByBill = redeemableLineAmount * p.maxRedeemPct() / 100;
        int maxByBill = (int) (maxAmountByBill / p.pointValueVnd());
        int max = Math.min(balance, maxByBill);
        if (balance < p.minRedeemPoints())
            return new RedeemQuote(false, 0, 0, "Số dư dưới " + p.minRedeemPoints() + " điểm");
        if (max < p.minRedeemPoints())
            return new RedeemQuote(false, 0, 0, "Giá trị hàng được đổi điểm trên hóa đơn chưa đủ để dùng tối thiểu " + p.minRedeemPoints() + " điểm");
        return new RedeemQuote(true, max, (long) max * p.pointValueVnd(), null);
    }

    /** BRL-10: điểm có hiệu lực đến hết tháng thứ 12 kể từ tháng tích (tính cả tháng tích). */
    public static OffsetDateTime expiresAt(OffsetDateTime earnedAt, int expiryMonths) {
        YearMonth ym = YearMonth.from(earnedAt.atZoneSameInstant(VN)).plusMonths(expiryMonths - 1L);
        return ym.atEndOfMonth().atTime(LocalTime.of(23, 59, 59)).atZone(VN).toOffsetDateTime();
    }

    /** BRL-11: hạng cao nhất có ngưỡng ≤ chi tiêu 12 tháng. */
    public static Tier tierFor(long spend12m, List<Tier> tiers) {
        return tiers.stream().filter(t -> spend12m >= t.thresholdSpend())
                .max(Comparator.comparingLong(Tier::thresholdSpend)).orElse(tiers.getFirst());
    }

    /** Hạng thấp hơn một bậc (BRL-11: hạ 1 bậc). */
    public static Tier oneLevelDown(Tier current, List<Tier> tiers) {
        return tiers.stream().filter(t -> t.thresholdSpend() < current.thresholdSpend())
                .max(Comparator.comparingLong(Tier::thresholdSpend)).orElse(current);
    }
}
