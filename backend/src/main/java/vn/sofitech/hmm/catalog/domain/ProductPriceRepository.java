package vn.sofitech.hmm.catalog.domain;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public interface ProductPriceRepository extends JpaRepository<ProductPrice, Long> {

    /** Các bản giá đã duyệt đang hiệu lực tại :at cho cửa hàng (gồm giá riêng cửa hàng và giá toàn chuỗi). */
    @Query("""
            select pp from ProductPrice pp
            where pp.product.id in :productIds
              and pp.status = vn.sofitech.hmm.catalog.domain.ProductPrice.Status.APPROVED
              and pp.effectiveFrom <= :at and (pp.effectiveTo is null or pp.effectiveTo > :at)
              and (pp.storeId = :storeId or pp.storeId is null)
            """)
    List<ProductPrice> findEffective(@Param("productIds") Collection<Long> productIds, @Param("storeId") Long storeId,
                                     @Param("at") OffsetDateTime at);

    /** Giá toàn chuỗi đang hiệu lực (danh sách mặt hàng trên Portal). */
    @Query("""
            select pp from ProductPrice pp
            where pp.product.id in :productIds and pp.storeId is null
              and pp.status = vn.sofitech.hmm.catalog.domain.ProductPrice.Status.APPROVED
              and pp.effectiveFrom <= :at and (pp.effectiveTo is null or pp.effectiveTo > :at)
            """)
    List<ProductPrice> findEffectiveChainPrices(@Param("productIds") Collection<Long> productIds, @Param("at") OffsetDateTime at);

    List<ProductPrice> findByProductIdOrderByEffectiveFromDescIdDesc(Long productId);

    @EntityGraph(attributePaths = "product")
    List<ProductPrice> findByStatusOrderByEffectiveFrom(ProductPrice.Status status);

    @Query("select pp.product.id, count(pp) from ProductPrice pp where pp.product.id in :productIds and pp.status = :status group by pp.product.id")
    List<Object[]> countByProductAndStatus(@Param("productIds") Collection<Long> productIds, @Param("status") ProductPrice.Status status);

    /** Bản giá đã duyệt cùng phạm vi đang còn hiệu lực tại thời điểm bản giá mới bắt đầu. */
    @Query("""
            select pp from ProductPrice pp
            where pp.product.id = :productId
              and pp.status = vn.sofitech.hmm.catalog.domain.ProductPrice.Status.APPROVED
              and ((:storeId is null and pp.storeId is null) or pp.storeId = :storeId)
              and pp.effectiveFrom < :from and (pp.effectiveTo is null or pp.effectiveTo > :from)
            """)
    List<ProductPrice> findOverlapping(@Param("productId") Long productId, @Param("storeId") Long storeId,
                                       @Param("from") OffsetDateTime from);
}
