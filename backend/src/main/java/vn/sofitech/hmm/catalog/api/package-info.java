/**
 * Kiểu dữ liệu công khai của module catalog: module khác dùng khi gọi {@link vn.sofitech.hmm.catalog.CatalogFacade}.
 * {@code @NamedInterface} báo cho Spring Modulith (ModularityTest) rằng thư mục này được phép dùng từ module khác.
 */
@NamedInterface("api")
package vn.sofitech.hmm.catalog.api;

import org.springframework.modulith.NamedInterface;
