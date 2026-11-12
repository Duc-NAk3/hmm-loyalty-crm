package vn.sofitech.hmm.catalog.web;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** DTO của module catalog. */
public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record CategoryView(Long categoryId, String code, String name, Long parentId, boolean promoRestricted) {
    }

    public record ProductView(Long productId, String sku, String name, String unit, Long categoryId, String categoryName,
                              BigDecimal vatRate, boolean promoRestricted, boolean categoryPromoRestricted, boolean pointExcluded,
                              String status, String barcodes, Long currentPrice, long draftPrices) {
    }

    public record ProductInput(String sku, String name, String unit, long categoryId, BigDecimal vatRate,
                               boolean promoRestricted, boolean pointExcluded, List<String> barcodes, String status) {
    }

    public record ImportResult(int created, int updated, List<String> errors) {
    }

    public record PriceView(Long priceId, Long storeId, String storeCode, long price, OffsetDateTime effectiveFrom,
                            OffsetDateTime effectiveTo, String status, String approvedByName) {
    }

    public record PendingPriceView(Long priceId, Long productId, String sku, String name, Long storeId, String storeCode,
                                   long price, OffsetDateTime effectiveFrom) {
    }

    public record PriceRequest(Long storeId, long price, OffsetDateTime effectiveFrom) {
    }

    public record IdResponse(Long id) {
    }

    public record StatusResponse(Long id, String status) {
    }
}
