package vn.sofitech.hmm.catalog.repository;

import vn.sofitech.hmm.catalog.domain.ProductBarcode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductBarcodeRepository extends JpaRepository<ProductBarcode, String> {
}
