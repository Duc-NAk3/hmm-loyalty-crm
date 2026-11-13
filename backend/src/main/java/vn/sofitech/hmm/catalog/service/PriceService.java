package vn.sofitech.hmm.catalog.service;

import vn.sofitech.hmm.admin.api.StoreInfo;
import vn.sofitech.hmm.admin.api.UserInfo;
import vn.sofitech.hmm.catalog.dto.price.PriceDto;
import vn.sofitech.hmm.catalog.dto.price.PendingPriceDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sofitech.hmm.admin.AdminFacade;
import vn.sofitech.hmm.catalog.domain.Product;
import vn.sofitech.hmm.catalog.domain.ProductPrice;
import vn.sofitech.hmm.catalog.repository.ProductPriceRepository;
import vn.sofitech.hmm.catalog.repository.ProductRepository;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.AuditService;
import vn.sofitech.hmm.shared.Texts;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Giá bán có thời điểm hiệu lực và phê duyệt (FR-12): tạo bản DRAFT → duyệt → bản cũ cùng phạm vi đóng hiệu lực. */
@Service
public class PriceService {

    private final ProductRepository products;
    private final ProductPriceRepository prices;
    private final AdminFacade admin;
    private final AuditService audit;

    public PriceService(ProductRepository products, ProductPriceRepository prices, AdminFacade admin, AuditService audit) {
        this.products = products;
        this.prices = prices;
        this.admin = admin;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<PriceDto> history(long productId) {
        return prices.findByProductIdOrderByEffectiveFromDescIdDesc(productId).stream().map(pp -> new PriceDto(pp.getId(),
                pp.getStoreId(), storeCode(pp.getStoreId()), pp.getPrice(), pp.getEffectiveFrom(), pp.getEffectiveTo(),
                pp.getStatus().name(),
                pp.getApprovedBy() == null ? null : admin.findUser(pp.getApprovedBy()).map(UserInfo::getFullName).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PendingPriceDto> pending() {
        return prices.findByStatusOrderByEffectiveFrom(ProductPrice.Status.DRAFT).stream().map(pp -> new PendingPriceDto(pp.getId(),
                pp.getProduct().getId(), pp.getProduct().getSku(), pp.getProduct().getName(), pp.getStoreId(),
                storeCode(pp.getStoreId()), pp.getPrice(), pp.getEffectiveFrom())).toList();
    }

    @Transactional
    public long create(long productId, Long storeId, long price, OffsetDateTime effectiveFrom, long userId) {
        Product p = products.findById(productId).orElseThrow(() -> ApiException.notFound("Không tìm thấy mặt hàng"));
        if (storeId != null && admin.findStore(storeId).isEmpty()) throw ApiException.badRequest("STORE", "Cửa hàng không tồn tại");
        ProductPrice pp = prices.save(new ProductPrice(p, storeId, price, effectiveFrom == null ? OffsetDateTime.now(Texts.VN) : effectiveFrom));
        audit.log(userId, "CREATE_PRICE", "product_prices", pp.getId(), null, Map.of("productId", productId, "price", price,
                "storeId", String.valueOf(storeId)));
        return pp.getId();
    }

    @Transactional
    public void approve(long priceId, long userId) {
        ProductPrice pp = prices.findById(priceId).orElseThrow(() -> ApiException.notFound("Không tìm thấy bản giá"));
        prices.findOverlapping(pp.getProduct().getId(), pp.getStoreId(), pp.getEffectiveFrom())
                .forEach(old -> old.closeAt(pp.getEffectiveFrom()));
        pp.approve(userId);
        audit.log(userId, "CHANGE_PRICE", "product_prices", priceId, null, Map.of("productId", pp.getProduct().getId(), "price", pp.getPrice()));
    }

    @Transactional
    public void reject(long priceId, long userId) {
        ProductPrice pp = prices.findById(priceId).orElseThrow(() -> ApiException.notFound("Không tìm thấy bản giá"));
        if (!pp.isDraft()) throw ApiException.conflict("PRICE_STATUS", "Chỉ hủy được bản giá chưa duyệt");
        prices.delete(pp);
        audit.log(userId, "REJECT_PRICE", "product_prices", priceId, null, null);
    }

    private String storeCode(Long storeId) {
        return storeId == null ? null : admin.findStore(storeId).map(StoreInfo::getStoreCode).orElse(null);
    }
}
