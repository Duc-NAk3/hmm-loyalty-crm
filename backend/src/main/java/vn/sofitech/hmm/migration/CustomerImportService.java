package vn.sofitech.hmm.migration;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import vn.sofitech.hmm.customer.CustomerService;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.AuditService;
import vn.sofitech.hmm.shared.Texts;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Nhập khách hàng từ file xuất của 3 POS cũ (FR-58), chuẩn hóa và kiểm tra chất lượng (FR-59),
 * tạo hồ sơ Chưa xác thực, chưa có consent marketing; trùng SĐT chuẩn hóa thì gộp vào hồ sơ có sẵn (FR-60, BRL-12 quy tắc 1).
 */
@Service
public class CustomerImportService {

    /** Ánh xạ cột theo định dạng xuất của từng nhà cung cấp POS cũ. */
    public record VendorFormat(String code, String label, char delimiter, String ref, String name, String phone, String dob,
                               String dobPattern, String email, String gender, Map<String, String> genderMap) {
    }

    public static final List<VendorFormat> VENDORS = List.of(
            new VendorFormat("POS_ALPHA", "POS Alpha (các cửa hàng miền Bắc)", ',', "ma_kh", "ho_ten", "so_dien_thoai", "ngay_sinh",
                    "dd/MM/yyyy", "email", "gioi_tinh", Map.of("nam", "MALE", "nu", "FEMALE")),
            new VendorFormat("POS_BETA", "POS Beta (các cửa hàng miền Trung)", ';', "CustomerCode", "FullName", "Mobile", "Birthday",
                    "yyyy-MM-dd", "Email", "Gender", Map.of("m", "MALE", "f", "FEMALE")),
            new VendorFormat("POS_GAMMA", "POS Gamma (các cửa hàng miền Nam)", ',', "id", "name", "phone", "dob",
                    "dd-MM-yyyy", "mail", "sex", Map.of("male", "MALE", "female", "FEMALE")));

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final JdbcClient jdbc;
    private final CustomerService customers;
    private final AuditService audit;
    private final JsonMapper json;

    public CustomerImportService(JdbcClient jdbc, CustomerService customers, AuditService audit, JsonMapper json) {
        this.jdbc = jdbc;
        this.customers = customers;
        this.audit = audit;
        this.json = json;
    }

    @Transactional
    public Map<String, Object> importCustomers(long storeId, String vendorCode, String fileName, byte[] content, long userId) throws IOException {
        VendorFormat v = VENDORS.stream().filter(x -> x.code().equals(vendorCode)).findFirst()
                .orElseThrow(() -> ApiException.badRequest("VENDOR", "Nhà cung cấp POS cũ không hợp lệ"));
        String text = new String(content, StandardCharsets.UTF_8);
        if (text.startsWith("﻿")) text = text.substring(1);
        List<String> lines = text.lines().toList();
        if (lines.isEmpty()) throw ApiException.badRequest("EMPTY", "File rỗng");

        // Lưu file gốc (kho lưu file – bản demo dùng thư mục uploads)
        Path dir = Path.of("uploads");
        Files.createDirectories(dir);
        long batchId = jdbc.sql("""
                INSERT INTO migration.import_batches (store_id, source_vendor, data_type, file_path, status, imported_by, imported_at)
                VALUES (:s, :v, 'CUSTOMER', '', 'UPLOADED', :u, now()) RETURNING batch_id
                """).param("s", storeId).param("v", v.code()).param("u", userId).query(Long.class).single();
        Path saved = dir.resolve("batch-" + batchId + "-" + fileName.replaceAll("[^A-Za-z0-9._-]", "_"));
        Files.write(saved, content);

        List<String> header = parseCsvLine(lines.getFirst(), v.delimiter()).stream().map(String::trim).toList();
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < header.size(); i++) idx.put(header.get(i), i);
        for (String required : List.of(v.name(), v.phone()))
            if (!idx.containsKey(required))
                throw ApiException.badRequest("FORMAT", "File không đúng định dạng " + v.label() + ": thiếu cột " + required);

        DateTimeFormatter dobFmt = DateTimeFormatter.ofPattern(v.dobPattern());
        int total = 0, created = 0, merged = 0, errors = 0;
        for (int rowNo = 2; rowNo <= lines.size(); rowNo++) {
            String line = lines.get(rowNo - 1);
            if (line.isBlank()) continue;
            total++;
            List<String> cells = parseCsvLine(line, v.delimiter());
            Map<String, String> raw = new LinkedHashMap<>();
            for (int i = 0; i < header.size(); i++) raw.put(header.get(i), i < cells.size() ? cells.get(i).trim() : "");

            List<String> problems = new ArrayList<>();
            String name = Texts.normalizeName(raw.getOrDefault(v.name(), ""));
            if (Texts.isBlank(name)) problems.add("Thiếu họ tên");
            String rawPhone = raw.getOrDefault(v.phone(), "");
            String phone = Texts.normalizePhone(rawPhone);
            if (phone == null) problems.add("SĐT không hợp lệ: '" + rawPhone + "'");
            LocalDate dob = null;
            String rawDob = v.dob() == null ? "" : raw.getOrDefault(v.dob(), "");
            if (!rawDob.isBlank()) {
                try {
                    dob = LocalDate.parse(rawDob, dobFmt);
                    if (dob.isAfter(LocalDate.now()) || dob.getYear() < 1900) problems.add("Ngày sinh ngoài khoảng hợp lệ: " + rawDob);
                } catch (DateTimeParseException e) {
                    problems.add("Ngày sinh sai định dạng " + v.dobPattern() + ": '" + rawDob + "'");
                }
            }
            String email = raw.getOrDefault(v.email(), "");
            if (!email.isBlank() && !EMAIL.matcher(email).matches()) problems.add("Email không hợp lệ: '" + email + "'");
            // Trùng SĐT ngay trong file: dòng sau được gộp vào hồ sơ tạo từ dòng trước (cùng cách xử lý trùng với hồ sơ có sẵn)
            String rawJson = json.writeValueAsString(raw);
            if (!problems.isEmpty()) {
                errors++;
                jdbc.sql("INSERT INTO migration.import_errors (batch_id, row_no, message, raw_data) VALUES (:b, :r, :m, CAST(:raw AS jsonb))")
                        .param("b", batchId).param("r", rowNo).param("m", String.join("; ", problems)).param("raw", rawJson).update();
                continue;
            }
            String gender = v.gender() == null ? null : v.genderMap().get(Texts.unaccent(raw.getOrDefault(v.gender(), "")));
            Optional<CustomerService.Customer> existing = customers.findActiveByPhone(phone);
            long customerId;
            if (existing.isPresent()) {
                customerId = existing.get().customerId();
                // Gộp: bổ sung thông tin còn trống của hồ sơ có sẵn, giữ nguyên thông tin đã có
                jdbc.sql("""
                        UPDATE customer.customers SET date_of_birth = COALESCE(date_of_birth, :dob), email = COALESCE(email, :email),
                               gender = COALESCE(gender, :gender), updated_at = now() WHERE customer_id = :id
                        """).param("dob", dob).param("email", email.isBlank() ? null : email).param("gender", gender).param("id", customerId).update();
                merged++;
            } else {
                customerId = customers.create(new CustomerService.NewCustomer(phone, name, gender, dob, email, storeId, "UNVERIFIED", null));
                created++;
            }
            jdbc.sql("""
                    INSERT INTO migration.customer_source_records (customer_id, batch_id, source_vendor, source_customer_ref, raw_data)
                    VALUES (:c, :b, :v, :ref, CAST(:raw AS jsonb))
                    """).param("c", customerId).param("b", batchId).param("v", v.code()).param("ref", raw.get(v.ref())).param("raw", rawJson).update();
        }
        jdbc.sql("UPDATE migration.import_batches SET status = 'IMPORTED', total_rows = :t, error_rows = :e, file_path = :f WHERE batch_id = :b")
                .param("t", total).param("e", errors).param("f", saved.toString().replace('\\', '/')).param("b", batchId).update();
        audit.log(userId, "IMPORT_DATA", "import_batches", batchId, null, Map.of("vendor", v.code(), "rows", total, "created", created, "merged", merged, "errors", errors));
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("batchId", batchId);
        res.put("totalRows", total);
        res.put("created", created);
        res.put("merged", merged);
        res.put("errorRows", errors);
        res.put("errors", errors(batchId));
        return res;
    }

    public List<Map<String, Object>> batches() {
        return jdbc.sql("""
                SELECT b.batch_id, s.store_code, b.source_vendor, b.data_type, b.status, b.total_rows, b.error_rows, b.imported_at,
                       u.username AS imported_by,
                       (SELECT count(*) FROM migration.customer_source_records r WHERE r.batch_id = b.batch_id) AS imported_rows
                FROM migration.import_batches b JOIN admin.stores s ON s.store_id = b.store_id
                LEFT JOIN admin.app_users u ON u.user_id = b.imported_by ORDER BY b.batch_id DESC
                """).query().listOfRows();
    }

    public List<Map<String, Object>> errors(long batchId) {
        return jdbc.sql("SELECT row_no, message, raw_data::text AS raw_data FROM migration.import_errors WHERE batch_id = :b ORDER BY row_no")
                .param("b", batchId).query().listOfRows();
    }

    /** Tách một dòng CSV, hỗ trợ giá trị trong dấu ngoặc kép. */
    static List<String> parseCsvLine(String line, char delimiter) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else if (ch == '"') quoted = false;
                else cur.append(ch);
            } else if (ch == '"') quoted = true;
            else if (ch == delimiter) {
                out.add(cur.toString());
                cur.setLength(0);
            } else cur.append(ch);
        }
        out.add(cur.toString());
        return out;
    }
}
