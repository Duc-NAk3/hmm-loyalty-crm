package vn.sofitech.hmm.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductBarcodeRepository extends JpaRepository<ProductBarcode, String> {
}
