/**
 * Kiểu dữ liệu công khai của module customer: module khác dùng khi gọi {@link vn.sofitech.hmm.customer.CustomerFacade}.
 * {@code @NamedInterface} báo cho Spring Modulith (ModularityTest) rằng thư mục này được phép dùng từ module khác.
 */
@NamedInterface("api")
package vn.sofitech.hmm.customer.api;

import org.springframework.modulith.NamedInterface;
