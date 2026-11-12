package vn.sofitech.hmm.catalog.dto.price;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PriceView {

    private Long priceId;
    private Long storeId;
    private String storeCode;
    private long price;
    private OffsetDateTime effectiveFrom;
    private OffsetDateTime effectiveTo;
    private String status;
    private String approvedByName;
}
