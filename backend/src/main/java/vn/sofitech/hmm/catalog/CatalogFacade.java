package vn.sofitech.hmm.catalog;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/** API công khai của module catalog (Facade): dữ liệu mặt hàng và giá để bán hàng. */
public interface CatalogFacade {

    /** Mặt hàng kèm giá đang hiệu lực tại một cửa hàng; price = -1 nếu chưa có giá (FR-02). */
    record SellableProduct(long productId, String sku, String name, String unit, String categoryName,
                           BigDecimal vatRate, boolean promoRestricted, boolean pointExcluded, long price, String barcode) {
        /** Dòng hàng được tích điểm và được trừ bằng điểm (BRL-05, Q-06). */
        public boolean pointEligible() {
            return !promoRestricted && !pointExcluded;
        }
    }

    record ProductLabel(long productId, String sku, String name, String unit, String categoryName) {
    }

    Map<Long, SellableProduct> sellableByIds(Collection<Long> productIds, long storeId);

    Map<Long, ProductLabel> labels(Collection<Long> productIds);
}
