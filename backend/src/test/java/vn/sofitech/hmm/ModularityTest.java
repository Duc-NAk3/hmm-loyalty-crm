package vn.sofitech.hmm;

import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Spring Modulith: kiểm tra ranh giới module của modular monolith (Architecture Proposal).
 * Báo lỗi nếu có phụ thuộc vòng hoặc module dùng lớp nội bộ (domain/, service/, web/) của module khác –
 * module khác chỉ được gọi qua Facade và DTO ở gốc package của module.
 * Bỏ qua package demo: công cụ nạp dữ liệu mẫu cần ghi trực tiếp vào mọi bảng.
 */
class ModularityTest {

    static final ApplicationModules MODULES = ApplicationModules.of(HmmLoyaltyApplication.class,
            JavaClass.Predicates.resideInAPackage("vn.sofitech.hmm.demo.."));

    @Test
    void verifiesModuleBoundaries() {
        MODULES.forEach(System.out::println);
        MODULES.verify();
    }
}
