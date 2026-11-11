package vn.sofitech.hmm.catalog;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.sofitech.hmm.admin.CurrentUser;
import vn.sofitech.hmm.shared.ApiException;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    // ---------------- POS (FR-02) ----------------

    /** Quét mã vạch hoặc tìm theo tên / mã hàng; giá theo cửa hàng của thu ngân. */
    @GetMapping("/api/pos/products")
    public List<CatalogService.SellableProduct> posSearch(@RequestParam(required = false) String barcode,
                                                          @RequestParam(required = false) String q,
                                                          @AuthenticationPrincipal Jwt jwt) {
        long storeId = CurrentUser.of(jwt).storeId();
        if (barcode != null && !barcode.isBlank())
            return catalog.findByBarcode(barcode, storeId).map(List::of).orElse(List.of());
        if (q == null || q.isBlank()) return List.of();
        return catalog.search(q, storeId);
    }

    // ---------------- Portal (FR-11, FR-12) ----------------

    @GetMapping("/api/portal/categories")
    public List<Map<String, Object>> categories() {
        return catalog.categories();
    }

    @GetMapping("/api/portal/products")
    public Map<String, Object> products(@RequestParam(required = false) String q, @RequestParam(required = false) Long categoryId,
                                        @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return catalog.listProducts(q, categoryId, page, Math.min(size, 100));
    }

    @PostMapping("/api/portal/products")
    @PreAuthorize("hasAnyRole('CATALOG','ADMIN')")
    public Map<String, Object> create(@RequestBody CatalogService.ProductInput in, @AuthenticationPrincipal Jwt jwt) {
        return Map.of("productId", catalog.saveProduct(null, in, CurrentUser.of(jwt).id()));
    }

    @PutMapping("/api/portal/products/{id}")
    @PreAuthorize("hasAnyRole('CATALOG','ADMIN')")
    public Map<String, Object> update(@PathVariable long id, @RequestBody CatalogService.ProductInput in, @AuthenticationPrincipal Jwt jwt) {
        return Map.of("productId", catalog.saveProduct(id, in, CurrentUser.of(jwt).id()));
    }

    @PostMapping("/api/portal/products/import")
    @PreAuthorize("hasAnyRole('CATALOG','ADMIN')")
    public Map<String, Object> importCsv(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal Jwt jwt) throws IOException {
        if (file.isEmpty()) throw ApiException.badRequest("EMPTY", "Chưa chọn file");
        return catalog.importProducts(file.getInputStream(), CurrentUser.of(jwt).id());
    }

    @GetMapping("/api/portal/products/{id}/prices")
    public List<Map<String, Object>> prices(@PathVariable long id) {
        return catalog.priceHistory(id);
    }

    public record PriceRequest(Long storeId, long price, OffsetDateTime effectiveFrom) {
    }

    @PostMapping("/api/portal/products/{id}/prices")
    @PreAuthorize("hasAnyRole('CATALOG','ADMIN')")
    public Map<String, Object> newPrice(@PathVariable long id, @RequestBody PriceRequest req, @AuthenticationPrincipal Jwt jwt) {
        return Map.of("priceId", catalog.createPrice(id, req.storeId(), req.price(), req.effectiveFrom(), CurrentUser.of(jwt).id()));
    }

    @GetMapping("/api/portal/prices/pending")
    public List<Map<String, Object>> pending() {
        return catalog.pendingPrices();
    }

    /** Duyệt giá: chỉ Quản trị (vai trò phê duyệt trong demo). */
    @PostMapping("/api/portal/prices/{priceId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> approve(@PathVariable long priceId, @AuthenticationPrincipal Jwt jwt) {
        catalog.approvePrice(priceId, CurrentUser.of(jwt).id());
        return Map.of("priceId", priceId, "status", "APPROVED");
    }

    @PostMapping("/api/portal/prices/{priceId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> reject(@PathVariable long priceId, @AuthenticationPrincipal Jwt jwt) {
        catalog.rejectPrice(priceId, CurrentUser.of(jwt).id());
        return Map.of("priceId", priceId, "status", "REJECTED");
    }
}
