package vn.sofitech.hmm.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Chuẩn hóa SĐT, họ tên, che dữ liệu (FR-18, FR-59). */
class TextsTest {

    @ParameterizedTest
    @CsvSource({
            "0901234567, 0901234567",
            "+84901234567, 0901234567",
            "84901234567, 0901234567",
            "'090 123 4567', 0901234567",
            "'090.123.4567', 0901234567",
            "901234567, 0901234567",
            "0321234567, 0321234567"
    })
    void normalizesValidPhones(String raw, String expected) {
        assertThat(Texts.normalizePhone(raw)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"012345678", "0241234567", "090123456", "abc", "''", "09012345678"})
    void rejectsInvalidPhones(String raw) {
        assertThat(Texts.normalizePhone(raw)).isNull();
    }

    @Test
    void masksPhone() {
        assertThat(Texts.maskPhone("0901234567")).isEqualTo("090****567");
    }

    @Test
    void unaccentForSearch() {
        assertThat(Texts.unaccent("  Nguyễn   Thị Đào ")).isEqualTo("nguyen thi dao");
    }

    @Test
    void normalizesName() {
        assertThat(Texts.normalizeName("  nguyễn  văn AN ")).isEqualTo("Nguyễn Văn An");
        assertThat(Texts.normalizeName("ĐẶNG thị ánh")).isEqualTo("Đặng Thị Ánh");
    }
}
