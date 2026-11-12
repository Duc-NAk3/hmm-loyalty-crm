package vn.sofitech.hmm.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.sofitech.hmm.shared.ApiException;

import java.time.OffsetDateTime;

/**
 * Giá bán theo thời điểm hiệu lực (catalog.product_prices). storeId = NULL nghĩa là giá toàn chuỗi.
 * Giá đã duyệt không sửa: muốn đổi giá thì tạo bản mới, bản cũ được đóng hiệu lực (FR-12).
 */
@Entity
@Table(schema = "catalog", name = "product_prices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductPrice {

    public enum Status { DRAFT, APPROVED }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "price_seq")
    @SequenceGenerator(name = "price_seq", schema = "catalog", sequenceName = "product_prices_price_id_seq", allocationSize = 50)
    @Column(name = "price_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    /** Cửa hàng áp dụng (module admin) – chỉ giữ ID. */
    @Column(name = "store_id")
    private Long storeId;

    @Column(nullable = false)
    private long price;

    @Column(name = "effective_from", nullable = false)
    private OffsetDateTime effectiveFrom;

    @Column(name = "effective_to")
    private OffsetDateTime effectiveTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "approved_by")
    private Long approvedBy;

    public ProductPrice(Product product, Long storeId, long price, OffsetDateTime effectiveFrom) {
        if (price <= 0) throw ApiException.badRequest("PRICE", "Giá phải lớn hơn 0");
        this.product = product;
        this.storeId = storeId;
        this.price = price;
        this.effectiveFrom = effectiveFrom;
        this.status = Status.DRAFT;
    }

    public void approve(long userId) {
        if (status != Status.DRAFT) throw ApiException.conflict("PRICE_STATUS", "Bản giá đã được duyệt");
        this.status = Status.APPROVED;
        this.approvedBy = userId;
    }

    /** Đóng hiệu lực khi có bản giá mới cùng phạm vi bắt đầu. */
    public void closeAt(OffsetDateTime at) {
        this.effectiveTo = at;
    }

    public boolean isDraft() {
        return status == Status.DRAFT;
    }
}
