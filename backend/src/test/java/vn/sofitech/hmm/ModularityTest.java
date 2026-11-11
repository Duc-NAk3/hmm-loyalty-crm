package vn.sofitech.hmm;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Spring Modulith: kiểm tra ranh giới module của modular monolith (Architecture Proposal).
 * Báo lỗi nếu có phụ thuộc vòng giữa các module hoặc module gọi vào phần nội bộ của module khác.
 */
class ModularityTest {

    static final ApplicationModules MODULES = ApplicationModules.of(HmmLoyaltyApplication.class);

    @Test
    void verifiesModuleBoundaries() {
        MODULES.forEach(System.out::println);
        MODULES.verify();
    }
}
