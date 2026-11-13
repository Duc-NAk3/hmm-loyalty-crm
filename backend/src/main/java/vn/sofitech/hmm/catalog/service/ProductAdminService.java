package vn.sofitech.hmm.catalog.service;

import vn.sofitech.hmm.catalog.dto.category.CategoryDto;
import vn.sofitech.hmm.catalog.dto.product.ProductDto;
import vn.sofitech.hmm.catalog.dto.product.ProductInputDto;
import vn.sofitech.hmm.catalog.dto.product.ImportResultDto;
import vn.sofitech.hmm.catalog.repository.CategoryRepository;
import vn.sofitech.hmm.catalog.repository.ProductBarcodeRepository;
import vn.sofitech.hmm.catalog.repository.ProductPriceRepository;
import vn.sofitech.hmm.catalog.repository.ProductRepository;
import vn.sofitech.hmm.catalog.repository.ProductSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sofitech.hmm.catalog.domain.*;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.AuditService;
import vn.sofitech.hmm.shared.PageResult;
import vn.sofitech.hmm.shared.Texts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** Quản lý mặt hàng trên Portal (FR-11): danh sách, tạo, sửa, nhập hàng loạt từ CSV. */
@Service
public class ProductAdminService {

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final ProductBarcodeRepository barcodes;
    private final ProductPriceRepository prices;
    private final PriceService priceService;
    private final AuditService audit;

    public ProductAdminService(ProductRepository products, CategoryRepository categories, ProductBarcodeRepository barcodes,
                               ProductPriceRepository prices, PriceService priceService, AuditService audit) {
        this.products = products;
        this.categories = categories;
        this.barcodes = barcodes;
        this.prices = prices;
        this.priceService = priceService;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> categories() {
        return categories.findAllByOrderByCode().stream().map(c -> new CategoryDto(c.getId(), c.getCode(), c.getName(),
                c.getParent() == null ? null : c.getParent().getId(), c.isPromoRestricted())).toList();
    }

    @Transactional(readOnly = true)
    public PageResult<ProductDto> list(String q, Long categoryId, int page, int size) {
        // Chỉ ghép điều kiện người dùng thật sự chọn; không lọc gì thì lấy tất cả
        List<Specification<Product>> filters = new ArrayList<>();
        if (!Texts.isBlank(q)) filters.add(ProductSpecifications.keyword(q.trim()));
        if (categoryId != null) filters.add(ProductSpecifications.inCategory(categoryId));
        Page<Product> result = products.findAll(Specification.allOf(filters), PageRequest.of(page, size, Sort.by("sku")));
        List<Long> ids = result.getContent().stream().map(Product::getId).toList();
        Map<Long, Long> current = new HashMap<>();
        if (!ids.isEmpty())
            prices.findEffectiveChainPrices(ids, OffsetDateTime.now(Texts.VN)).stream()
                    .sorted(Comparator.comparing(ProductPrice::getEffectiveFrom))
                    .forEach(pp -> current.put(pp.getProduct().getId(), pp.getPrice()));
        Map<Long, Long> drafts = new HashMap<>();
        if (!ids.isEmpty())
            for (Object[] row : prices.countByProductAndStatus(ids, ProductPrice.Status.DRAFT)) drafts.put((Long) row[0], (Long) row[1]);
        return new PageResult<>(result.getTotalElements(), result.getContent().stream().map(p -> new ProductDto(p.getId(), p.getSku(),
                p.getName(), p.getUnit(), p.getCategory().getId(), p.getCategory().getName(), p.getVatRate(), p.isPromoRestricted(),
                p.getCategory().isPromoRestricted(), p.isPointExcluded(), p.getStatus(),
                p.getBarcodes().stream().map(ProductBarcode::getBarcode).sorted().collect(Collectors.joining(", ")),
                current.get(p.getId()), drafts.getOrDefault(p.getId(), 0L))).toList());
    }

    @Transactional
    public long save(Long productId, ProductInputDto in, long userId) {
        if (Texts.isBlank(in.getSku()) || Texts.isBlank(in.getName()) || Texts.isBlank(in.getUnit()))
            throw ApiException.badRequest("PRODUCT", "Mã hàng, tên hàng và đơn vị tính là bắt buộc");
        Category category = categories.findById(in.getCategoryId()).orElseThrow(() -> ApiException.badRequest("CATEGORY", "Ngành hàng không tồn tại"));
        List<String> codes = in.getBarcodes() == null ? List.of()
                : in.getBarcodes().stream().map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
        for (String code : codes)
            barcodes.findById(code).filter(b -> !b.getProduct().getId().equals(productId)).ifPresent(b -> {
                throw ApiException.conflict("BARCODE", "Mã vạch " + code + " đã thuộc mặt hàng khác");
            });
        BigDecimal vat = in.getVatRate() == null ? BigDecimal.valueOf(8) : in.getVatRate();
        Product p;
        if (productId == null) {
            p = new Product(in.getSku().trim(), in.getName().trim(), in.getUnit().trim(), category, vat, in.isPromoRestricted(), in.isPointExcluded());
        } else {
            p = products.findById(productId).orElseThrow(() -> ApiException.notFound("Không tìm thấy mặt hàng"));
            p.update(in.getSku().trim(), in.getName().trim(), in.getUnit().trim(), category, vat, in.isPromoRestricted(), in.isPointExcluded(), in.getStatus());
        }
        p.replaceBarcodes(codes);
        p = products.save(p);
        audit.log(userId, productId == null ? "CREATE_PRODUCT" : "UPDATE_PRODUCT", "products", p.getId(), null,
                Map.of("sku", p.getSku(), "name", p.getName()));
        return p.getId();
    }

    /**
     * Nhập hàng loạt mặt hàng từ CSV (FR-11). Cột: sku,name,unit,category_code,vat_rate,barcodes,promo_restricted,point_excluded,price.
     * barcodes cách nhau bằng "|", để trống khi cập nhật thì giữ mã vạch cũ; price (nếu có) tạo giá toàn chuỗi đã duyệt, hiệu lực ngay.
     */
    @Transactional
    public ImportResultDto importCsv(InputStream in, long userId) throws IOException {
        List<String> errors = new ArrayList<>();
        int created = 0, updated = 0, row = 0;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            if (r.readLine() == null) throw ApiException.badRequest("EMPTY", "File rỗng");
            String line;
            while ((line = r.readLine()) != null) {
                row++;
                if (line.isBlank()) continue;
                String[] c = line.split(",", -1);
                try {
                    if (c.length < 8) throw new IllegalArgumentException("thiếu cột");
                    Category cat = categories.findByCode(c[3].trim())
                            .orElseThrow(() -> new IllegalArgumentException("ngành hàng " + c[3] + " không tồn tại"));
                    Optional<Product> existing = products.findBySku(c[0].trim());
                    List<String> codes = c[5].isBlank() && existing.isPresent()
                            ? existing.get().getBarcodes().stream().map(ProductBarcode::getBarcode).toList()
                            : Arrays.asList(c[5].split("\\|"));
                    long id = save(existing.map(Product::getId).orElse(null), new ProductInputDto(c[0], c[1], c[2], cat.getId(),
                            new BigDecimal(c[4].trim()), Boolean.parseBoolean(c[6].trim()), Boolean.parseBoolean(c[7].trim()), codes, "ACTIVE"), userId);
                    if (c.length > 8 && !c[8].isBlank()) {
                        long priceId = priceService.create(id, null, Long.parseLong(c[8].trim()), OffsetDateTime.now(Texts.VN), userId);
                        priceService.approve(priceId, userId);
                    }
                    if (existing.isPresent()) updated++;
                    else created++;
                } catch (ApiException | IllegalArgumentException e) {
                    errors.add("Dòng " + (row + 1) + ": " + e.getMessage());
                }
            }
        }
        return new ImportResultDto(created, updated, errors);
    }
}
