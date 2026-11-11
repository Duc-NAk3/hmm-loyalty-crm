package vn.sofitech.hmm.admin;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/** Tham số hệ thống cấu hình được (FR-68), ví dụ OTP_TTL_MINUTES. */
@Service
public class ParameterService {

    private final JdbcClient jdbc;

    public ParameterService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public int getInt(String key, int defaultValue) {
        return jdbc.sql("SELECT param_value FROM admin.system_parameters WHERE param_key = :k").param("k", key)
                .query(String.class).optional().map(Integer::parseInt).orElse(defaultValue);
    }
}
