package vn.sofitech.hmm.catalog.dto.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

    private Long productId;
    private String sku;
    private String name;
    private String unit;
    private Long categoryId;
    private String categoryName;
    private BigDecimal vatRate;
    private boolean promoRestricted;
    private boolean categoryPromoRestricted;
    private boolean pointExcluded;
    private String status;
    private String barcodes;
    private Long currentPrice;
    private long draftPrices;
}
