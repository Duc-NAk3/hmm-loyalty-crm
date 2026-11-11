package vn.sofitech.hmm.loyalty;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.sofitech.hmm.loyalty.PointCalculator.Policy;
import vn.sofitech.hmm.loyalty.PointCalculator.RedeemQuote;
import vn.sofitech.hmm.loyalty.PointCalculator.Tier;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Kiểm tra công thức điểm theo BA mục 7 (BRL-03, 04, 08, 09, 10, 11). */
class PointCalculatorTest {

    static final Policy POLICY = new Policy(1, 10_000, 100, 50, 50, 12, 15);
    static final List<Tier> TIERS = List.of(
            new Tier("MEMBER", "Thành viên", 0, new BigDecimal("1.00")),
            new Tier("SILVER", "Bạc", 3_000_000, new BigDecimal("1.20")),
            new Tier("GOLD", "Vàng", 10_000_000, new BigDecimal("1.50")),
            new Tier("DIAMOND", "Kim cương", 30_000_000, new BigDecimal("2.00")));

    @Test
    @DisplayName("BRL-03: điểm cơ bản = làm tròn xuống (giá trị tính điểm / 10.000đ)")
    void basePointsRoundDown() {
        assertThat(PointCalculator.earnedPoints(0, POLICY, BigDecimal.ONE, BigDecimal.ONE)).isZero();
        assertThat(PointCalculator.earnedPoints(9_999, POLICY, BigDecimal.ONE, BigDecimal.ONE)).isZero();
        assertThat(PointCalculator.earnedPoints(10_000, POLICY, BigDecimal.ONE, BigDecimal.ONE)).isEqualTo(1);
        assertThat(PointCalculator.earnedPoints(259_000, POLICY, BigDecimal.ONE, BigDecimal.ONE)).isEqualTo(25);
    }

    @Test
    @DisplayName("BRL-04: điểm thực nhận = làm tròn xuống (điểm cơ bản × hệ số hạng × hệ số chương trình)")
    void tierAndPromotionMultipliers() {
        // 259.000đ → 25 điểm cơ bản; Bạc x1,2 → 30; Vàng x1,5 → 37,5 → 37; Kim cương x2 trong ngày x2 → 100
        assertThat(PointCalculator.earnedPoints(259_000, POLICY, new BigDecimal("1.20"), BigDecimal.ONE)).isEqualTo(30);
        assertThat(PointCalculator.earnedPoints(259_000, POLICY, new BigDecimal("1.50"), BigDecimal.ONE)).isEqualTo(37);
        assertThat(PointCalculator.earnedPoints(259_000, POLICY, new BigDecimal("2.00"), new BigDecimal("2"))).isEqualTo(100);
    }

    @Test
    @DisplayName("BRL-08: tối đa 50% giá trị hàng được phép, 1 điểm = 100đ, không vượt số dư")
    void quoteCapsAtHalfOfBillAndBalance() {
        RedeemQuote byBill = PointCalculator.quote(181_500, 5_000, POLICY);
        assertThat(byBill.allowed()).isTrue();
        assertThat(byBill.maxPoints()).isEqualTo(907);          // 90.750đ / 100 → 907 điểm
        assertThat(byBill.maxAmount()).isEqualTo(90_700);

        RedeemQuote byBalance = PointCalculator.quote(1_000_000, 320, POLICY);
        assertThat(byBalance.maxPoints()).isEqualTo(320);
    }

    @Test
    @DisplayName("BRL-09: số dư hoặc giá trị hóa đơn dưới mức tối thiểu 50 điểm thì không được đổi")
    void quoteRejectsBelowMinimum() {
        assertThat(PointCalculator.quote(1_000_000, 49, POLICY).allowed()).isFalse();
        RedeemQuote smallBill = PointCalculator.quote(9_000, 1_000, POLICY); // 50% = 4.500đ = 45 điểm < 50
        assertThat(smallBill.allowed()).isFalse();
        assertThat(smallBill.reason()).contains("tối thiểu 50");
        assertThat(PointCalculator.quote(0, 1_000, POLICY).allowed()).isFalse(); // hóa đơn chỉ có hàng hạn chế khuyến mại
    }

    @Test
    @DisplayName("BRL-10: điểm hết hạn cuối tháng thứ 12 kể từ tháng tích")
    void expiryEndOfTwelfthMonth() {
        OffsetDateTime earned = OffsetDateTime.of(2025, 12, 1, 10, 0, 0, 0, ZoneOffset.ofHours(7));
        assertThat(PointCalculator.expiresAt(earned, 12))
                .isEqualTo(OffsetDateTime.of(2026, 11, 30, 23, 59, 59, 0, ZoneOffset.ofHours(7)));
        OffsetDateTime lateMonth = OffsetDateTime.of(2026, 1, 31, 22, 0, 0, 0, ZoneOffset.ofHours(7));
        assertThat(PointCalculator.expiresAt(lateMonth, 12).toLocalDate()).hasToString("2026-12-31");
    }

    @Test
    @DisplayName("BRL-11: hạng theo chi tiêu 12 tháng; hạ hạng chỉ 1 bậc")
    void tiers() {
        assertThat(PointCalculator.tierFor(0, TIERS).code()).isEqualTo("MEMBER");
        assertThat(PointCalculator.tierFor(2_999_999, TIERS).code()).isEqualTo("MEMBER");
        assertThat(PointCalculator.tierFor(3_000_000, TIERS).code()).isEqualTo("SILVER");
        assertThat(PointCalculator.tierFor(45_000_000, TIERS).code()).isEqualTo("DIAMOND");
        assertThat(PointCalculator.oneLevelDown(TIERS.get(3), TIERS).code()).isEqualTo("GOLD");
        assertThat(PointCalculator.oneLevelDown(TIERS.get(0), TIERS).code()).isEqualTo("MEMBER");
    }
}
