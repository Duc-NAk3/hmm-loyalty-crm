package vn.sofitech.hmm.admin.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Định nghĩa OpenAPI cho Swagger UI (/swagger-ui.html): khai báo xác thực Bearer JWT để Swagger hiện nút "Authorize"
 * và tự gắn header Authorization vào mọi lời gọi thử. Token lấy từ POST /api/auth/login.
 */
@Configuration
public class OpenApiConfig {

    static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI hmmOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("HMM Loyalty CRM API")
                        .version("demo")
                        .description("API của bản demo Loyalty CRM Hoa Mai Mart. Gọi POST /api/auth/login để lấy token, "
                                + "bấm Authorize và dán token (không cần chữ Bearer). Tài khoản demo: mật khẩu Demo@123."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT nhận từ POST /api/auth/login (trường token)")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
