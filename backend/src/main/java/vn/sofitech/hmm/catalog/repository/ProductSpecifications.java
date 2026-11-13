package vn.sofitech.hmm.catalog.repository;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import vn.sofitech.hmm.catalog.domain.Product;
import vn.sofitech.hmm.catalog.domain.ProductBarcode;

/**
 * Bộ lọc mặt hàng dạng Specification (Spring Data JPA, dựa trên Criteria API).
 * Mỗi hàm là một điều kiện; nơi gọi chỉ ghép những điều kiện người dùng thật sự chọn
 * ({@code Specification.allOf(...)}), nên câu SQL không có tham số null và Hibernate luôn biết kiểu dữ liệu.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    /** Từ khóa khớp mã hàng, một phần tên (không phân biệt hoa thường) hoặc một mã vạch của mặt hàng. */
    public static Specification<Product> keyword(String keyword) {
        return (product, query, cb) -> {
            Subquery<Long> barcode = query.subquery(Long.class);
            Root<ProductBarcode> b = barcode.from(ProductBarcode.class);
            barcode.select(cb.literal(1L))
                    .where(cb.equal(b.get("product"), product), cb.equal(b.get("barcode"), keyword));
            return cb.or(
                    cb.equal(product.get("sku"), keyword),
                    cb.like(cb.lower(product.get("name")), "%" + keyword.toLowerCase() + "%"),
                    cb.exists(barcode));
        };
    }

    /** Thuộc ngành hàng. */
    public static Specification<Product> inCategory(long categoryId) {
        return (product, query, cb) -> cb.equal(product.get("category").get("id"), categoryId);
    }
}
