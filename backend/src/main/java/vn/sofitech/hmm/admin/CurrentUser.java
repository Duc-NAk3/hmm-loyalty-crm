package vn.sofitech.hmm.admin;

import org.springframework.security.oauth2.jwt.Jwt;
import vn.sofitech.hmm.shared.ApiException;

/** Người dùng đang đăng nhập, đọc từ JWT. storeId = null nghĩa là phạm vi toàn chuỗi. */
public record CurrentUser(Long id, String username, String fullName, String role, Long storeId) {

    public static CurrentUser of(Jwt jwt) {
        if (jwt == null) throw ApiException.forbidden("Chưa đăng nhập");
        Number uid = jwt.getClaim("uid");
        Number sid = jwt.getClaim("storeId");
        return new CurrentUser(uid.longValue(), jwt.getSubject(), jwt.getClaimAsString("name"),
                jwt.getClaimAsString("role"), sid == null ? null : sid.longValue());
    }

    public boolean is(String r) {
        return r.equals(role);
    }

    /** Quản lý cửa hàng chỉ xem được cửa hàng của mình; vai trò trung tâm xem toàn chuỗi. */
    public Long scopeStore(Long requested) {
        if (storeId != null && is("STORE_MANAGER")) return storeId;
        return requested;
    }
}
