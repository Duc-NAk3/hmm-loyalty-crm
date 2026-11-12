package vn.sofitech.hmm.admin;

import java.util.Optional;

/**
 * API công khai của module admin (Facade pattern). Module khác chỉ gọi qua interface này,
 * không truy cập trực tiếp entity hay repository của admin.
 */
public interface AdminFacade {

    record TerminalInfo(Long terminalId, String terminalCode, Long storeId, String storeCode, String storeName) {
    }

    record StoreInfo(Long storeId, String storeCode, String name, String address) {
    }

    record UserInfo(Long userId, String username, String fullName) {
    }

    /** Quầy đang hoạt động. */
    Optional<TerminalInfo> findActiveTerminal(long terminalId);

    /** Quầy bất kể trạng thái (dùng khi in lại hóa đơn cũ). */
    Optional<TerminalInfo> findTerminal(long terminalId);

    Optional<StoreInfo> findStore(long storeId);

    Optional<UserInfo> findUser(long userId);

    /** Tham số hệ thống kiểu số nguyên (FR-68), ví dụ OTP_TTL_MINUTES. */
    int intParam(String key, int defaultValue);
}
