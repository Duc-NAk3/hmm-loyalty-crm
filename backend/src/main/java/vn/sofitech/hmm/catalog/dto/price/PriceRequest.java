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
public class PriceRequest {

    private Long storeId;
    private long price;
    private OffsetDateTime effectiveFrom;
}
