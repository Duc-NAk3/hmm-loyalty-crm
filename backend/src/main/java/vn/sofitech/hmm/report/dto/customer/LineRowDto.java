package vn.sofitech.hmm.report.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LineRowDto {

    private String sku;
    private String name;
    private String category;
    private BigDecimal quantity;
    private long unitPrice;
    private long lineAmount;
    private boolean pointEligible;
}
