package vn.sofitech.hmm.loyalty.dto.member;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Kết quả tra cứu thành viên tại quầy (FR-18): SĐT che một phần, hạng, điểm khả dụng, điểm sắp hết hạn. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MemberSummaryDto {

    private Long customerId;
    private String memberCode;
    private String fullName;
    private String phoneMasked;
    private String status;
    private boolean verified;
    private boolean redeemLocked;
    private String tierCode;
    private String tierName;
    private BigDecimal tierMultiplier;
    private int pointBalance;
    private long pointValueVnd;
    private long expiringPoints;
    private OffsetDateTime expiringAt;
    private long spend12m;
    private String nextTierName;
    private long spendToNextTier;
}
