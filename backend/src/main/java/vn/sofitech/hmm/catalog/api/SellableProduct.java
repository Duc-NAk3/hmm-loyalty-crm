package vn.sofitech.hmm.catalog.api;

import lombok.Value;

import java.math.BigDecimal;

/** Mặt hàng kèm giá đang hiệu lực tại một cửa hàng; price = -1 nếu chưa có giá (FR-02). */
@Value
public class SellableProduct {

    long productId;
    String sku;
    String name;
    String unit;
    String categoryName;
    BigDecimal vatRate;
    boolean promoRestricted;
    boolean pointExcluded;
    long price;
    String barcode;

    /** Dòng hàng được tích điểm và được trừ bằng điểm (BRL-05, Q-06). */
    public boolean pointEligible() {
        return !promoRestricted && !pointExcluded;
    }
}
