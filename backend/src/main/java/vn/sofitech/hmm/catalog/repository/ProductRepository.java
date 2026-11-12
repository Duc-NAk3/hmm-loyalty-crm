package vn.sofitech.hmm.catalog.repository;

import vn.sofitech.hmm.catalog.domain.Product;
import vn.sofitech.hmm.catalog.domain.ProductBarcode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    @EntityGraph(attributePaths = "category")
    List<Product> findAllByIdIn(Collection<Long> ids);

    /** Quét mã vạch tại quầy. */
    @Query("select b.product from ProductBarcode b where b.barcode = :barcode and b.product.status = 'ACTIVE'")
    Optional<Product> findActiveByBarcode(@Param("barcode") String barcode);

    /** Tìm theo mã hàng, mã vạch hoặc một phần tên (không phân biệt hoa thường). */
    @Query("""
            select p from Product p join fetch p.category
            where p.status = 'ACTIVE'
              and (p.sku = :q or lower(p.name) like lower(concat('%', :q, '%'))
                   or exists (select 1 from ProductBarcode b where b.product = p and b.barcode = :q))
            order by p.name
            """)
    List<Product> searchActive(@Param("q") String q, Pageable page);

    /** Danh sách mặt hàng trên Portal, lọc theo từ khóa và ngành hàng. */
    @Query(value = """
            select p from Product p join fetch p.category c
            where (:q is null or p.sku = :q or lower(p.name) like lower(concat('%', :q, '%'))
                   or exists (select 1 from ProductBarcode b where b.product = p and b.barcode = :q))
              and (:categoryId is null or c.id = :categoryId)
            """,
            countQuery = """
            select count(p) from Product p
            where (:q is null or p.sku = :q or lower(p.name) like lower(concat('%', :q, '%'))
                   or exists (select 1 from ProductBarcode b where b.product = p and b.barcode = :q))
              and (:categoryId is null or p.category.id = :categoryId)
            """)
    Page<Product> searchPage(@Param("q") String q, @Param("categoryId") Long categoryId, Pageable page);
}
