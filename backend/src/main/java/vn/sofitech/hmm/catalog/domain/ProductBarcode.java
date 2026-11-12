package vn.sofitech.hmm.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Mã vạch của mặt hàng; một mặt hàng có thể có nhiều mã (catalog.product_barcodes). */
@Entity
@Table(schema = "catalog", name = "product_barcodes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductBarcode {

    @Id
    private String barcode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    ProductBarcode(String barcode, Product product) {
        this.barcode = barcode;
        this.product = product;
    }
}
