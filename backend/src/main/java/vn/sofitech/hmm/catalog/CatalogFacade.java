package vn.sofitech.hmm.catalog;

import vn.sofitech.hmm.catalog.api.ProductLabel;
import vn.sofitech.hmm.catalog.api.SellableProduct;

import java.util.Collection;
import java.util.Map;

/** API công khai của module catalog (Facade): dữ liệu mặt hàng và giá để bán hàng. */
public interface CatalogFacade {

    Map<Long, SellableProduct> sellableByIds(Collection<Long> productIds, long storeId);

    Map<Long, ProductLabel> labels(Collection<Long> productIds);
}
