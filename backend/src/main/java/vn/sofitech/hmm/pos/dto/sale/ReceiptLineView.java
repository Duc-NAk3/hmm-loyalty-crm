package vn.sofitech.hmm.pos.dto.sale;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptLineView {

    private String sku;
    private String name;
    private String unit;
    private BigDecimal quantity;
    private long unitPrice;
    private long lineAmount;
    private boolean pointEligible;
}
