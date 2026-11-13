package vn.sofitech.hmm.loyalty.dto.tier;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TierDto {

    private String tierCode;
    private String name;
    private long thresholdSpend;
    private BigDecimal multiplier;
    private String benefits;
    private long members;
}
