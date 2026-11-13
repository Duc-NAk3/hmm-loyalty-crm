package vn.sofitech.hmm.catalog.api;

import lombok.Value;

@Value
public class ProductLabel {

    long productId;
    String sku;
    String name;
    String unit;
    String categoryName;
}
