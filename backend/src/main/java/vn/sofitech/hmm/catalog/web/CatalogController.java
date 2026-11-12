package vn.sofitech.hmm.catalog.web;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.sofitech.hmm.admin.CurrentUser;
import vn.sofitech.hmm.catalog.CatalogFacade.SellableProduct;
import vn.sofitech.hmm.catalog.service.CatalogService;
import vn.sofitech.hmm.catalog.service.PriceService;
import vn.sofitech.hmm.catalog.service.ProductAdminService;
import vn.sofitech.hmm.catalog.web.CatalogDtos.*;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.PageResult;

import java.io.IOException;
import java.util.List;

@RestController
public class CatalogController {

    private final CatalogService catalog;
    private final ProductAdminService productAdmin;
    private final PriceService priceService;

    public CatalogController(CatalogService catalog, ProductAdminService productAdmin, PriceService priceService) {
        this.catalog = catalog;
        this.productAdmin = productAdmin;
        this.priceService = priceService;
    }

    // ---------------- POS (FR-02) ----------------

    /** Quét mã vạch hoặc tìm theo tên / mã hàng; giá theo cửa hàng của thu ngân. */
    @GetMapping("/api/pos/products")
    public List<SellableProduct> posSearch(@RequestParam(required = false) String barcode, @RequestParam(required = false) String q,
                                           @AuthenticationPrincipal Jwt jwt) {
        long storeId = CurrentUser.of(jwt).storeId();
        if (barcode != null && !barcode.isBlank()) return catalog.findByBarcode(barcode, storeId).map(List::of).orElse(List.of());
        if (q == null || q.isBlank()) return List.of();
        return catalog.search(q, storeId);
    }

    // ---------------- Portal (FR-11, FR-12) ----------------

    @GetMapping("/api/portal/categories")
    public List<CategoryView> categories() {
        return productAdmin.categories();
    }

    @GetMapping("/api/portal/products")
    public PageResult<ProductView> products(@RequestParam(required = false) String q, @RequestParam(required = false) Long categoryId,
                                            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return productAdmin.list(q, categoryId, page, Math.min(size, 100));
    }

    @PostMapping("/api/portal/products")
    @PreAuthorize("hasAnyRole('CATALOG','ADMIN')")
    public IdResponse create(@RequestBody ProductInput in, @AuthenticationPrincipal Jwt jwt) {
        return new IdResponse(productAdmin.save(null, in, CurrentUser.of(jwt).id()));
    }

    @PutMapping("/api/portal/products/{id}")
    @PreAuthorize("hasAnyRole('CATALOG','ADMIN')")
    public IdResponse update(@PathVariable long id, @RequestBody ProductInput in, @AuthenticationPrincipal Jwt jwt) {
        return new IdResponse(productAdmin.save(id, in, CurrentUser.of(jwt).id()));
    }

    @PostMapping("/api/portal/products/import")
    @PreAuthorize("hasAnyRole('CATALOG','ADMIN')")
    public ImportResult importCsv(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal Jwt jwt) throws IOException {
        if (file.isEmpty()) throw ApiException.badRequest("EMPTY", "Chưa chọn file");
        return productAdmin.importCsv(file.getInputStream(), CurrentUser.of(jwt).id());
    }

    @GetMapping("/api/portal/products/{id}/prices")
    public List<PriceView> prices(@PathVariable long id) {
        return priceService.history(id);
    }

    @PostMapping("/api/portal/products/{id}/prices")
    @PreAuthorize("hasAnyRole('CATALOG','ADMIN')")
    public IdResponse newPrice(@PathVariable long id, @RequestBody PriceRequest req, @AuthenticationPrincipal Jwt jwt) {
        return new IdResponse(priceService.create(id, req.storeId(), req.price(), req.effectiveFrom(), CurrentUser.of(jwt).id()));
    }

    @GetMapping("/api/portal/prices/pending")
    public List<PendingPriceView> pending() {
        return priceService.pending();
    }

    /** Duyệt giá: chỉ Quản trị (vai trò phê duyệt trong demo). */
    @PostMapping("/api/portal/prices/{priceId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public StatusResponse approve(@PathVariable long priceId, @AuthenticationPrincipal Jwt jwt) {
        priceService.approve(priceId, CurrentUser.of(jwt).id());
        return new StatusResponse(priceId, "APPROVED");
    }

    @PostMapping("/api/portal/prices/{priceId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public StatusResponse reject(@PathVariable long priceId, @AuthenticationPrincipal Jwt jwt) {
        priceService.reject(priceId, CurrentUser.of(jwt).id());
        return new StatusResponse(priceId, "REJECTED");
    }
}
