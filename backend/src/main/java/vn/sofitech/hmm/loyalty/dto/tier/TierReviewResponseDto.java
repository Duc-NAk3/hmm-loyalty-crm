package vn.sofitech.hmm.loyalty.dto.tier;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TierReviewResponseDto {

    private int customersChecked;
    private int spendRecalculated;
    private int upgraded;
    private int downgraded;
    private OffsetDateTime ranAt;
}
