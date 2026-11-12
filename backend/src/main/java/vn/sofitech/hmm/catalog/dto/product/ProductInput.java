package vn.sofitech.hmm.catalog.dto.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductInput {

    private String sku;
    private String name;
    private String unit;
    private long categoryId;
    private BigDecimal vatRate;
    private boolean promoRestricted;
    private boolean pointExcluded;
    private List<String> barcodes;
    private String status;
}
