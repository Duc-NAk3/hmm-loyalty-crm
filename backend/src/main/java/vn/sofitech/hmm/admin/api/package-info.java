/**
 * Kiểu dữ liệu công khai của module admin: module khác dùng khi gọi {@link vn.sofitech.hmm.admin.AdminFacade}.
 * {@code @NamedInterface} báo cho Spring Modulith (ModularityTest) rằng thư mục này được phép dùng từ module khác.
 */
@NamedInterface("api")
package vn.sofitech.hmm.admin.api;

import org.springframework.modulith.NamedInterface;
