package vn.sofitech.hmm.migration.service;

import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Định dạng file xuất của từng nhà cung cấp POS cũ (Strategy): dấu phân cách, tên cột, định dạng ngày sinh, mã giới tính.
 * Thêm nhà cung cấp mới chỉ cần khai báo một VendorFormat, không sửa logic nhập.
 */
public record VendorFormat(String code, String label, char delimiter, String ref, String name, String phone, String dob,
                           String dobPattern, String email, String gender, Map<String, String> genderMap) {

    public static final List<VendorFormat> ALL = List.of(
            new VendorFormat("POS_ALPHA", "POS Alpha (các cửa hàng miền Bắc)", ',', "ma_kh", "ho_ten", "so_dien_thoai", "ngay_sinh",
                    "dd/MM/yyyy", "email", "gioi_tinh", Map.of("nam", "MALE", "nu", "FEMALE")),
            new VendorFormat("POS_BETA", "POS Beta (các cửa hàng miền Trung)", ';', "CustomerCode", "FullName", "Mobile", "Birthday",
                    "yyyy-MM-dd", "Email", "Gender", Map.of("m", "MALE", "f", "FEMALE")),
            new VendorFormat("POS_GAMMA", "POS Gamma (các cửa hàng miền Nam)", ',', "id", "name", "phone", "dob",
                    "dd-MM-yyyy", "mail", "sex", Map.of("male", "MALE", "female", "FEMALE")));

    public static Optional<VendorFormat> of(String code) {
        return ALL.stream().filter(v -> v.code().equals(code)).findFirst();
    }

    /** STRICT: ngày không tồn tại (31/02) là lỗi, không tự làm tròn về cuối tháng. */
    public DateTimeFormatter dobFormatter() {
        return DateTimeFormatter.ofPattern(dobPattern.replace("yyyy", "uuuu")).withResolverStyle(ResolverStyle.STRICT);
    }

    public List<String> columns() {
        return List.of(ref, name, phone, dob, email, gender);
    }
}
