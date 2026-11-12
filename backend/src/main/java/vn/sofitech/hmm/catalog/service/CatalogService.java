package vn.sofitech.hmm.catalog.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sofitech.hmm.catalog.CatalogFacade;
import vn.sofitech.hmm.catalog.domain.Product;
import vn.sofitech.hmm.catalog.domain.ProductBarcode;
import vn.sofitech.hmm.catalog.domain.ProductPrice;
import vn.sofitech.hmm.catalog.repository.ProductPriceRepository;
import vn.sofitech.hmm.catalog.repository.ProductRepository;
import vn.sofitech.hmm.shared.Texts;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Dữ liệu bán hàng tại quầy: tra mã vạch, tìm hàng, giá đang hiệu lực theo cửa hàng (FR-02, FR-12). */
@Service
@Transactional(readOnly = true)
public class CatalogService implements CatalogFacade {

    private final ProductRepository products;
    private final ProductPriceRepository prices;

    public CatalogService(ProductRepository products, ProductPriceRepository prices) {
        this.products = products;
        this.prices = prices;
    }

    public Optional<SellableProduct> findByBarcode(String barcode, long storeId) {
        return products.findActiveByBarcode(barcode.trim()).map(p -> toSellable(List.of(p), storeId).getFirst());
    }

    public List<SellableProduct> search(String q, long storeId) {
        return toSellable(products.searchActive(q.trim(), PageRequest.of(0, 20)), storeId);
    }

    @Override
    public Map<Long, SellableProduct> sellableByIds(Collection<Long> productIds, long storeId) {
        if (productIds.isEmpty()) return Map.of();
        return toSellable(products.findAllByIdIn(productIds).stream().filter(Product::isActive).toList(), storeId).stream()
                .collect(Collectors.toMap(SellableProduct::productId, Function.identity()));
    }

    @Override
    public Map<Long, ProductLabel> labels(Collection<Long> productIds) {
        return products.findAllByIdIn(productIds).stream().collect(Collectors.toMap(Product::getId,
                p -> new ProductLabel(p.getId(), p.getSku(), p.getName(), p.getUnit(), p.getCategory().getName())));
    }

    /** Giá riêng cửa hàng được ưu tiên hơn giá toàn chuỗi; cùng phạm vi thì lấy bản hiệu lực mới nhất. */
    private List<SellableProduct> toSellable(List<Product> list, long storeId) {
        if (list.isEmpty()) return List.of();
        Map<Long, ProductPrice> best = new HashMap<>();
        Comparator<ProductPrice> priority = Comparator.<ProductPrice, Boolean>comparing(pp -> pp.getStoreId() != null)
                .thenComparing(ProductPrice::getEffectiveFrom);
        for (ProductPrice pp : prices.findEffective(list.stream().map(Product::getId).toList(), storeId, OffsetDateTime.now(Texts.VN)))
            best.merge(pp.getProduct().getId(), pp, (a, b) -> priority.compare(a, b) >= 0 ? a : b);
        return list.stream().map(p -> {
            ProductPrice pp = best.get(p.getId());
            String barcode = p.getBarcodes().stream().map(ProductBarcode::getBarcode).min(String::compareTo).orElse(null);
            return new SellableProduct(p.getId(), p.getSku(), p.getName(), p.getUnit(), p.getCategory().getName(), p.getVatRate(),
                    p.isRestricted(), p.isPointExcluded(), pp == null ? -1 : pp.getPrice(), barcode);
        }).toList();
    }
}
