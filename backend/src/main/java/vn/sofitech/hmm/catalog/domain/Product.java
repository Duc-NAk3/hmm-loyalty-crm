package vn.sofitech.hmm.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/** Mặt hàng (catalog.products) cùng các mã vạch của nó. */
@Entity
@Table(schema = "catalog", name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String unit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "vat_rate", nullable = false)
    private BigDecimal vatRate;

    /** Mặt hàng hạn chế khuyến mại: không tích, không đổi điểm (BRL-05). */
    @Column(name = "promo_restricted", nullable = false)
    private boolean promoRestricted;

    /** Không tích điểm theo quyết định của HMM (Q-06). */
    @Column(name = "point_excluded", nullable = false)
    private boolean pointExcluded;

    @Column(nullable = false)
    private String status;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("barcode")
    private List<ProductBarcode> barcodes = new ArrayList<>();

    public Product(String sku, String name, String unit, Category category, BigDecimal vatRate,
                   boolean promoRestricted, boolean pointExcluded) {
        update(sku, name, unit, category, vatRate, promoRestricted, pointExcluded, "ACTIVE");
    }

    public void update(String sku, String name, String unit, Category category, BigDecimal vatRate,
                       boolean promoRestricted, boolean pointExcluded, String status) {
        this.sku = sku;
        this.name = name;
        this.unit = unit;
        this.category = category;
        this.vatRate = vatRate;
        this.promoRestricted = promoRestricted;
        this.pointExcluded = pointExcluded;
        this.status = status == null ? "ACTIVE" : status;
    }

    /** Thay danh sách mã vạch: giữ mã còn dùng, xóa mã bỏ đi, thêm mã mới (tránh xóa rồi chèn lại cùng khóa). */
    public void replaceBarcodes(Collection<String> codes) {
        Set<String> wanted = Set.copyOf(codes);
        barcodes.removeIf(b -> !wanted.contains(b.getBarcode()));
        for (String code : codes)
            if (barcodes.stream().noneMatch(b -> b.getBarcode().equals(code))) barcodes.add(new ProductBarcode(code, this));
    }

    /** Hạn chế khuyến mại ở mức mặt hàng hoặc ngành hàng. */
    public boolean isRestricted() {
        return promoRestricted || category.isPromoRestricted();
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
