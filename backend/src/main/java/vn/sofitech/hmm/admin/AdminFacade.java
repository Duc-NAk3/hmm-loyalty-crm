package vn.sofitech.hmm.admin;

import vn.sofitech.hmm.admin.api.StoreInfo;
import vn.sofitech.hmm.admin.api.TerminalInfo;
import vn.sofitech.hmm.admin.api.UserInfo;

import java.util.Optional;

/**
 * API công khai của module admin (Facade pattern). Module khác chỉ gọi qua interface này,
 * không truy cập trực tiếp entity hay repository của admin.
 */
public interface AdminFacade {

    /** Quầy đang hoạt động. */
    Optional<TerminalInfo> findActiveTerminal(long terminalId);

    /** Quầy bất kể trạng thái (dùng khi in lại hóa đơn cũ). */
    Optional<TerminalInfo> findTerminal(long terminalId);

    Optional<StoreInfo> findStore(long storeId);

    Optional<UserInfo> findUser(long userId);

    /** Tham số hệ thống kiểu số nguyên (FR-68), ví dụ OTP_TTL_MINUTES. */
    int intParam(String key, int defaultValue);
}
