package vn.sofitech.hmm.pos.dto.redemption;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuoteResponseDto {

    private boolean allowed;
    private int maxPoints;
    private long maxAmount;
    private String reason;
    private long redeemableAmount;
}
