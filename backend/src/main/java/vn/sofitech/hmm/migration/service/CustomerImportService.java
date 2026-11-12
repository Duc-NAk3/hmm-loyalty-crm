package vn.sofitech.hmm.migration.service;

import vn.sofitech.hmm.migration.dto.customerimport.ErrorView;
import vn.sofitech.hmm.migration.dto.customerimport.ImportResult;
import vn.sofitech.hmm.migration.dto.customerimport.BatchView;
import vn.sofitech.hmm.migration.repository.CustomerSourceRecordRepository;
import vn.sofitech.hmm.migration.repository.ImportBatchRepository;
import vn.sofitech.hmm.migration.repository.ImportErrorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import vn.sofitech.hmm.admin.AdminFacade;
import vn.sofitech.hmm.customer.CustomerFacade;
import vn.sofitech.hmm.customer.CustomerFacade.CustomerInfo;
import vn.sofitech.hmm.migration.domain.*;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.AuditService;
import vn.sofitech.hmm.shared.Texts;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final ImportBatchRepository batches;
    private final ImportErrorRepository errors;
    private final CustomerSourceRecordRepository sources;
    private final CustomerFacade customers;
    private final AdminFacade admin;
    private final AuditService audit;
    private final JsonMapper json;

    public CustomerImportService(ImportBatchRepository batches, ImportErrorRepository errors, CustomerSourceRecordRepository sources,
                                 CustomerFacade customers, AdminFacade admin, AuditService audit, JsonMapper json) {
        this.batches = batches;
        this.errors = errors;
        this.sources = sources;
        this.customers = customers;
        this.admin = admin;
        this.audit = audit;
        this.json = json;
    }

    @Transactional
    public ImportResult importCustomers(long storeId, String vendorCode, String fileName, byte[] content, long userId) throws IOException {
        VendorFormat v = VendorFormat.of(vendorCode).orElseThrow(() -> ApiException.badRequest("VENDOR", "Nhà cung cấp POS cũ không hợp lệ"));
        if (admin.findStore(storeId).isEmpty()) throw ApiException.badRequest("STORE", "Cửa hàng không tồn tại");
        String text = new String(content, StandardCharsets.UTF_8);
        if (text.startsWith("﻿")) text = text.substring(1);
        List<String> lines = text.lines().toList();
        if (lines.isEmpty()) throw ApiException.badRequest("EMPTY", "File rỗng");

        List<String> header = parseCsvLine(lines.getFirst(), v.delimiter()).stream().map(String::trim).toList();
        for (String required : List.of(v.name(), v.phone()))
            if (!header.contains(required))
                throw ApiException.badRequest("FORMAT", "File không đúng định dạng " + v.label() + ": thiếu cột " + required);

        ImportBatch batch = batches.save(new ImportBatch(storeId, v.code(), "CUSTOMER", userId, OffsetDateTime.now(Texts.VN)));
        // Lưu file gốc (kho lưu file – bản demo dùng thư mục uploads)
        Path dir = Path.of("uploads");
        Files.createDirectories(dir);
        Path saved = dir.resolve("batch-" + batch.getId() + "-" + fileName.replaceAll("[^A-Za-z0-9._-]", "_"));
        Files.write(saved, content);

        DateTimeFormatter dobFmt = v.dobFormatter();
        int total = 0, created = 0, merged = 0, errorCount = 0;
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
            String rawDob = raw.getOrDefault(v.dob(), "");
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
            String rawJson = json.writeValueAsString(raw);
            if (!problems.isEmpty()) {
                errorCount++;
                errors.save(new ImportError(batch, rowNo, String.join("; ", problems), rawJson));
                continue;
            }
            String gender = v.genderMap().get(Texts.unaccent(raw.getOrDefault(v.gender(), "")));
            // Trùng SĐT với hồ sơ có sẵn (kể cả hồ sơ vừa tạo từ dòng trước trong cùng file) → gộp
            Optional<CustomerInfo> existing = customers.findActiveByPhone(phone);
            long customerId;
            if (existing.isPresent()) {
                customerId = existing.get().customerId();
                customers.fillMissing(customerId, dob, email, gender);
                merged++;
            } else {
                customerId = customers.create(new CustomerFacade.NewCustomer(phone, name, gender, dob, email, storeId, "UNVERIFIED", null));
                created++;
            }
            sources.save(new CustomerSourceRecord(customerId, batch, v.code(), raw.get(v.ref()), rawJson));
        }
        batch.complete(total, errorCount, saved.toString().replace('\\', '/'));
        audit.log(userId, "IMPORT_DATA", "import_batches", batch.getId(), null,
                Map.of("vendor", v.code(), "rows", total, "created", created, "merged", merged, "errors", errorCount));
        return new ImportResult(batch.getId(), total, created, merged, errorCount, errorsOf(batch.getId()));
    }

    @Transactional(readOnly = true)
    public List<BatchView> batches() {
        Map<Long, Long> imported = new HashMap<>();
        batches.countImportedPerBatch().forEach(r -> imported.put((Long) r[0], (Long) r[1]));
        return batches.findAllByOrderByIdDesc().stream().map(b -> new BatchView(b.getId(),
                admin.findStore(b.getStoreId()).map(AdminFacade.StoreInfo::storeCode).orElse(null), b.getSourceVendor(), b.getDataType(),
                b.getStatus().name(), b.getTotalRows(), b.getErrorRows(), b.getImportedAt(),
                b.getImportedBy() == null ? null : admin.findUser(b.getImportedBy()).map(AdminFacade.UserInfo::username).orElse(null),
                imported.getOrDefault(b.getId(), 0L))).toList();
    }

    @Transactional(readOnly = true)
    public List<ErrorView> errorsOf(long batchId) {
        return errors.findByBatchIdOrderByRowNo(batchId).stream().map(e -> new ErrorView(e.getRowNo(), e.getMessage(), e.getRawData())).toList();
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
