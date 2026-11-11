package vn.sofitech.hmm.shared;

import java.text.Normalizer;
import java.time.ZoneId;
import java.util.Locale;
import java.util.regex.Pattern;

/** Hàm tiện ích: chuẩn hóa SĐT, họ tên, che dữ liệu. */
public final class Texts {

    public static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Locale VI = Locale.forLanguageTag("vi");
    private static final Pattern MOBILE = Pattern.compile("^0[35789]\\d{8}$");
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");

    private Texts() {
    }

    /** Chuẩn hóa SĐT: bỏ ký tự thừa, đổi +84/84 thành 0. Trả về null nếu không hợp lệ. */
    public static String normalizePhone(String raw) {
        if (raw == null) return null;
        String d = raw.replaceAll("[^0-9]", "");
        if (d.startsWith("84") && d.length() == 11) d = "0" + d.substring(2);
        if (d.length() == 9 && !d.startsWith("0")) d = "0" + d;
        return MOBILE.matcher(d).matches() ? d : null;
    }

    /** 0901234567 → 090****567 */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }

    /** Họ tên không dấu, chữ thường – để tìm kiếm và so khớp trùng. */
    public static String unaccent(String s) {
        if (s == null) return null;
        String n = Normalizer.normalize(s.replace('đ', 'd').replace('Đ', 'D'), Normalizer.Form.NFD);
        return MARKS.matcher(n).replaceAll("").toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    /** "  nguyễn  văn AN " → "Nguyễn Văn An" */
    public static String normalizeName(String s) {
        if (s == null) return null;
        String[] parts = s.trim().replaceAll("\\s+", " ").toLowerCase(VI).split(" ");
        StringBuilder b = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (!b.isEmpty()) b.append(' ');
            b.append(p.substring(0, 1).toUpperCase(VI)).append(p.substring(1));
        }
        return b.toString();
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
