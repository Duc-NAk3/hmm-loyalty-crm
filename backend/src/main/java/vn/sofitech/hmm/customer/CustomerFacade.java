package vn.sofitech.hmm.customer;

import vn.sofitech.hmm.customer.api.CustomerInfo;
import vn.sofitech.hmm.customer.api.NewCustomer;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * API công khai của module customer (Facade). Hồ sơ khách do module này sở hữu; số dư điểm, chi tiêu và hạng
 * do module loyalty cập nhật thông qua các hàm dưới đây (có khóa dòng), không sửa trực tiếp entity.
 */
public interface CustomerFacade {

    Optional<CustomerInfo> findById(long customerId);

    /** Tra cứu theo SĐT (tự chuẩn hóa +84/84) hoặc mã thành viên; bỏ qua hồ sơ đã gộp (FR-18). */
    Optional<CustomerInfo> lookup(String query);

    Optional<CustomerInfo> findActiveByPhone(String normalizedPhone);

    /** Tạo hồ sơ: dùng chung cho đăng ký tại quầy, chuyển đổi dữ liệu. */
    long create(NewCustomer customer);

    /** Đăng ký nhanh tại quầy (FR-19): không OTP, hồ sơ Chưa xác thực, ghi consent, SMS chào mừng. */
    CustomerInfo registerAtPos(String phone, String fullName, String gender, LocalDate dateOfBirth, boolean marketingConsent,
                               long storeId, long cashierId);

    /** OTP thành công lần đầu → Đã xác thực (FR-20). Trả về true nếu vừa chuyển trạng thái. */
    boolean markVerified(long customerId);

    /** Cộng / trừ số dư điểm (khóa dòng); trừ quá số dư thì báo lỗi trừ khi allowNegative. Trả về số dư mới. */
    int adjustPointBalance(long customerId, int delta, boolean allowNegative);

    /** Cộng chi tiêu 12 tháng (khóa dòng); trả về hồ sơ sau khi cộng. */
    CustomerInfo addSpend(long customerId, long amount);

    void setSpend12m(long customerId, long spend);

    void changeTier(long customerId, String tierCode);

    /** Gộp dữ liệu chuyển đổi: chỉ bổ sung thông tin còn trống. */
    void fillMissing(long customerId, LocalDate dateOfBirth, String email, String gender);

    /** Hồ sơ còn hoạt động, dùng cho tác vụ xét hạng hằng ngày. */
    List<CustomerInfo> findAllForTierReview();

    /** Số khách theo hạng (bỏ hồ sơ đã gộp). */
    Map<String, Long> countByTier();
}
