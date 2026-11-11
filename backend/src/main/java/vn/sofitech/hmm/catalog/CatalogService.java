package vn.sofitech.hmm.catalog;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.AuditService;
import vn.sofitech.hmm.shared.Texts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;

/** Danh mục hàng hóa và giá bán tập trung (FR-11, FR-12). */
@Service
public class CatalogService {

    /** Mặt hàng kèm giá đang hiệu lực tại một cửa hàng – dữ liệu POS dùng để bán (FR-02). */
    public record SellableProduct(long productId, String sku, String name, String unit, String categoryName,
                                  BigDecimal vatRate, boolean promoRestricted, boolean pointExcluded,
                                  long price, String barcode) {
        /** Dòng hàng được tính điểm và được trừ bằng điểm (BRL-05, Q-06). */
        public boolean pointEligible() {
            return !promoRestricted && !pointExcluded;
        }
    }

    private static final String CURRENT_PRICE = """
            (SELECT pp.price FROM catalog.product_prices pp
             WHERE pp.product_id = p.product_id AND pp.status = 'APPROVED'
               AND pp.effective_from <= now() AND (pp.effective_to IS NULL OR pp.effective_to > now())
               AND (pp.store_id = :storeId OR pp.store_id IS NULL)
             ORDER BY (pp.store_id IS NULL), pp.effective_from DESC LIMIT 1)
            """;

    private final JdbcClient jdbc;
    private final AuditService audit;

    public CatalogService(JdbcClient jdbc, AuditService audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    // ---------------- bán hàng ----------------

    public Optional<SellableProduct> findByBarcode(String barcode, long storeId) {
        return querySellable("b.barcode = :q", barcode.trim(), storeId, 1).stream().findFirst();
    }

    public List<SellableProduct> search(String q, long storeId) {
        String term = q.trim();
        return querySellable("(p.sku = :q OR b.barcode = :q OR lower(p.name) LIKE '%' || lower(:q) || '%')", term, storeId, 20);
    }

    public Map<Long, SellableProduct> sellableByIds(Collection<Long> ids, long storeId) {
        if (ids.isEmpty()) return Map.of();
        Map<Long, SellableProduct> m = new HashMap<>();
        jdbc.sql(baseSellable("p.product_id IN (:ids)") + " ORDER BY p.product_id")
                .param("ids", ids).param("storeId", storeId).query(this::mapSellable).list()
                .forEach(p -> m.putIfAbsent(p.productId(), p));
        return m;
    }

    private List<SellableProduct> querySellable(String where, String q, long storeId, int limit) {
        List<SellableProduct> rows = jdbc.sql(baseSellable(where) + " ORDER BY p.name LIMIT :limit")
                .param("q", q).param("storeId", storeId).param("limit", limit * 3).query(this::mapSellable).list();
        Map<Long, SellableProduct> distinct = new LinkedHashMap<>();
        rows.forEach(r -> distinct.putIfAbsent(r.productId(), r));
        return distinct.values().stream().limit(limit).toList();
    }

    private String baseSellable(String where) {
        return """
                SELECT p.product_id, p.sku, p.name, p.unit, c.name AS category_name, p.vat_rate,
                       (p.promo_restricted OR c.promo_restricted) AS promo_restricted, p.point_excluded,
                       %s AS price,
                       (SELECT min(b2.barcode) FROM catalog.product_barcodes b2 WHERE b2.product_id = p.product_id) AS barcode
                FROM catalog.products p
                JOIN catalog.categories c ON c.category_id = p.category_id
                LEFT JOIN catalog.product_barcodes b ON b.product_id = p.product_id
                WHERE p.status = 'ACTIVE' AND %s
                """.formatted(CURRENT_PRICE, where);
    }

    private SellableProduct mapSellable(java.sql.ResultSet rs, int i) throws java.sql.SQLException {
        long price = rs.getLong("price");
        if (rs.wasNull()) price = -1;
        return new SellableProduct(rs.getLong("product_id"), rs.getString("sku"), rs.getString("name"), rs.getString("unit"),
                rs.getString("category_name"), rs.getBigDecimal("vat_rate"), rs.getBoolean("promo_restricted"),
                rs.getBoolean("point_excluded"), price, rs.getString("barcode"));
    }

    // ---------------- quản lý mặt hàng (portal) ----------------

    public List<Map<String, Object>> categories() {
        return jdbc.sql("SELECT category_id, code, name, parent_id, promo_restricted FROM catalog.categories ORDER BY code").query().listOfRows();
    }

    public Map<String, Object> listProducts(String q, Long categoryId, int page, int size) {
        String where = """
                WHERE (CAST(:q AS varchar) IS NULL OR lower(p.name) LIKE '%' || lower(:q) || '%' OR p.sku = :q
                       OR EXISTS (SELECT 1 FROM catalog.product_barcodes b WHERE b.product_id = p.product_id AND b.barcode = :q))
                  AND (CAST(:cat AS bigint) IS NULL OR p.category_id = :cat)
                """;
        String query = Texts.isBlank(q) ? null : q.trim();
        long total = jdbc.sql("SELECT count(*) FROM catalog.products p " + where).param("q", query).param("cat", categoryId).query(Long.class).single();
        List<Map<String, Object>> items = jdbc.sql("""
                SELECT p.product_id, p.sku, p.name, p.unit, p.category_id, c.name AS category_name, p.vat_rate,
                       p.promo_restricted, c.promo_restricted AS category_promo_restricted, p.point_excluded, p.status,
                       (SELECT string_agg(b.barcode, ', ' ORDER BY b.barcode) FROM catalog.product_barcodes b WHERE b.product_id = p.product_id) AS barcodes,
                       (SELECT pp.price FROM catalog.product_prices pp WHERE pp.product_id = p.product_id AND pp.store_id IS NULL
                          AND pp.status = 'APPROVED' AND pp.effective_from <= now() AND (pp.effective_to IS NULL OR pp.effective_to > now())
                        ORDER BY pp.effective_from DESC LIMIT 1) AS current_price,
                       (SELECT count(*) FROM catalog.product_prices pp WHERE pp.product_id = p.product_id AND pp.status = 'DRAFT') AS draft_prices
                FROM catalog.products p JOIN catalog.categories c ON c.category_id = p.category_id
                """ + where + " ORDER BY p.sku LIMIT :size OFFSET :offset")
                .param("q", query).param("cat", categoryId).param("size", size).param("offset", page * size).query().listOfRows();
        return Map.of("total", total, "items", items);
    }

    public record ProductInput(String sku, String name, String unit, long categoryId, BigDecimal vatRate,
                               boolean promoRestricted, boolean pointExcluded, List<String> barcodes, String status) {
    }

    @Transactional
    public long saveProduct(Long productId, ProductInput in, long userId) {
        if (Texts.isBlank(in.sku()) || Texts.isBlank(in.name()) || Texts.isBlank(in.unit()))
            throw ApiException.badRequest("PRODUCT", "Mã hàng, tên hàng và đơn vị tính là bắt buộc");
        List<String> barcodes = in.barcodes() == null ? List.of() : in.barcodes().stream().map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
        for (String b : barcodes) {
            Long owner = jdbc.sql("SELECT product_id FROM catalog.product_barcodes WHERE barcode = :b").param("b", b).query(Long.class).optional().orElse(null);
            if (owner != null && !owner.equals(productId))
                throw ApiException.conflict("BARCODE", "Mã vạch " + b + " đã thuộc mặt hàng khác");
        }
        long id;
        if (productId == null) {
            id = jdbc.sql("""
                    INSERT INTO catalog.products (sku, name, unit, category_id, vat_rate, promo_restricted, point_excluded, status)
                    VALUES (:sku, :name, :unit, :cat, :vat, :pr, :px, 'ACTIVE') RETURNING product_id
                    """).param("sku", in.sku().trim()).param("name", in.name().trim()).param("unit", in.unit().trim())
                    .param("cat", in.categoryId()).param("vat", in.vatRate() == null ? BigDecimal.valueOf(8) : in.vatRate())
                    .param("pr", in.promoRestricted()).param("px", in.pointExcluded()).query(Long.class).single();
        } else {
            id = productId;
            int n = jdbc.sql("""
                    UPDATE catalog.products SET sku = :sku, name = :name, unit = :unit, category_id = :cat, vat_rate = :vat,
                           promo_restricted = :pr, point_excluded = :px, status = :st WHERE product_id = :id
                    """).param("sku", in.sku().trim()).param("name", in.name().trim()).param("unit", in.unit().trim())
                    .param("cat", in.categoryId()).param("vat", in.vatRate()).param("pr", in.promoRestricted())
                    .param("px", in.pointExcluded()).param("st", in.status() == null ? "ACTIVE" : in.status()).param("id", id).update();
            if (n == 0) throw ApiException.notFound("Không tìm thấy mặt hàng");
            jdbc.sql("DELETE FROM catalog.product_barcodes WHERE product_id = :id").param("id", id).update();
        }
        for (String b : barcodes)
            jdbc.sql("INSERT INTO catalog.product_barcodes (barcode, product_id) VALUES (:b, :id)").param("b", b).param("id", id).update();
        audit.log(userId, productId == null ? "CREATE_PRODUCT" : "UPDATE_PRODUCT", "products", id, null, Map.of("sku", in.sku(), "name", in.name()));
        return id;
    }

    /**
     * Nhập hàng loạt mặt hàng từ CSV (FR-11). Cột: sku,name,unit,category_code,vat_rate,barcodes,promo_restricted,point_excluded,price
     * barcodes cách nhau bằng "|"; price (nếu có) tạo giá toàn chuỗi đã duyệt, hiệu lực ngay.
     */
    @Transactional
    public Map<String, Object> importProducts(InputStream in, long userId) throws IOException {
        List<String> errors = new ArrayList<>();
        int created = 0, updated = 0, row = 0;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String header = r.readLine();
            if (header == null) throw ApiException.badRequest("EMPTY", "File rỗng");
            String line;
            while ((line = r.readLine()) != null) {
                row++;
                if (line.isBlank()) continue;
                String[] c = line.split(",", -1);
                try {
                    if (c.length < 8) throw new IllegalArgumentException("thiếu cột");
                    Long catId = jdbc.sql("SELECT category_id FROM catalog.categories WHERE code = :c").param("c", c[3].trim())
                            .query(Long.class).optional().orElseThrow(() -> new IllegalArgumentException("ngành hàng " + c[3] + " không tồn tại"));
                    Long existing = jdbc.sql("SELECT product_id FROM catalog.products WHERE sku = :s").param("s", c[0].trim()).query(Long.class).optional().orElse(null);
                    ProductInput p = new ProductInput(c[0], c[1], c[2], catId, new BigDecimal(c[4].trim()),
                            Boolean.parseBoolean(c[6].trim()), Boolean.parseBoolean(c[7].trim()), Arrays.asList(c[5].split("\\|")), "ACTIVE");
                    long id = saveProduct(existing, p, userId);
                    if (c.length > 8 && !c[8].isBlank()) {
                        long price = Long.parseLong(c[8].trim());
                        long priceId = createPrice(id, null, price, OffsetDateTime.now(Texts.VN), userId);
                        approvePrice(priceId, userId);
                    }
                    if (existing == null) created++; else updated++;
                } catch (ApiException | IllegalArgumentException e) {
                    errors.add("Dòng " + (row + 1) + ": " + e.getMessage());
                }
            }
        }
        return Map.of("created", created, "updated", updated, "errors", errors);
    }

    // ---------------- giá bán ----------------

    public List<Map<String, Object>> priceHistory(long productId) {
        return jdbc.sql("""
                SELECT pp.price_id, pp.store_id, s.store_code, pp.price, pp.effective_from, pp.effective_to, pp.status,
                       u.full_name AS approved_by_name
                FROM catalog.product_prices pp LEFT JOIN admin.stores s ON s.store_id = pp.store_id
                LEFT JOIN admin.app_users u ON u.user_id = pp.approved_by
                WHERE pp.product_id = :id ORDER BY pp.effective_from DESC, pp.price_id DESC
                """).param("id", productId).query().listOfRows();
    }

    public List<Map<String, Object>> pendingPrices() {
        return jdbc.sql("""
                SELECT pp.price_id, pp.product_id, p.sku, p.name, pp.store_id, s.store_code, pp.price, pp.effective_from
                FROM catalog.product_prices pp JOIN catalog.products p ON p.product_id = pp.product_id
                LEFT JOIN admin.stores s ON s.store_id = pp.store_id
                WHERE pp.status = 'DRAFT' ORDER BY pp.effective_from
                """).query().listOfRows();
    }

    /** Tạo bản giá mới ở trạng thái DRAFT; giá đã có hiệu lực không sửa (FR-12). */
    @Transactional
    public long createPrice(long productId, Long storeId, long price, OffsetDateTime effectiveFrom, long userId) {
        if (price <= 0) throw ApiException.badRequest("PRICE", "Giá phải lớn hơn 0");
        if (effectiveFrom == null) effectiveFrom = OffsetDateTime.now(Texts.VN);
        long id = jdbc.sql("""
                INSERT INTO catalog.product_prices (product_id, store_id, price, effective_from, status)
                VALUES (:pid, :sid, :price, :from, 'DRAFT') RETURNING price_id
                """).param("pid", productId).param("sid", storeId).param("price", price).param("from", effectiveFrom).query(Long.class).single();
        audit.log(userId, "CREATE_PRICE", "product_prices", id, null, Map.of("productId", productId, "price", price, "storeId", String.valueOf(storeId)));
        return id;
    }

    /** Duyệt giá: bản giá trước đó cùng phạm vi được đóng hiệu lực tại thời điểm bản mới bắt đầu. */
    @Transactional
    public void approvePrice(long priceId, long userId) {
        Map<String, Object> p = jdbc.sql("SELECT product_id, store_id, price, effective_from, status FROM catalog.product_prices WHERE price_id = :id")
                .param("id", priceId).query().listOfRows().stream().findFirst().orElseThrow(() -> ApiException.notFound("Không tìm thấy bản giá"));
        if (!"DRAFT".equals(p.get("status"))) throw ApiException.conflict("PRICE_STATUS", "Bản giá đã được duyệt");
        Object storeId = p.get("store_id");
        Object from = p.get("effective_from");
        jdbc.sql("""
                UPDATE catalog.product_prices SET effective_to = :from
                WHERE product_id = :pid AND status = 'APPROVED' AND store_id IS NOT DISTINCT FROM CAST(:sid AS bigint)
                  AND effective_from < :from AND (effective_to IS NULL OR effective_to > :from)
                """).param("from", from).param("pid", p.get("product_id")).param("sid", storeId).update();
        jdbc.sql("UPDATE catalog.product_prices SET status = 'APPROVED', approved_by = :u WHERE price_id = :id")
                .param("u", userId).param("id", priceId).update();
        audit.log(userId, "CHANGE_PRICE", "product_prices", priceId, null, Map.of("productId", p.get("product_id"), "price", p.get("price")));
    }

    @Transactional
    public void rejectPrice(long priceId, long userId) {
        int n = jdbc.sql("DELETE FROM catalog.product_prices WHERE price_id = :id AND status = 'DRAFT'").param("id", priceId).update();
        if (n == 0) throw ApiException.conflict("PRICE_STATUS", "Chỉ hủy được bản giá chưa duyệt");
        audit.log(userId, "REJECT_PRICE", "product_prices", priceId, null, null);
    }
}
