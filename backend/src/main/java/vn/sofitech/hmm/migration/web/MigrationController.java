package vn.sofitech.hmm.migration.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.sofitech.hmm.admin.CurrentUser;
import vn.sofitech.hmm.migration.service.CustomerImportService;
import vn.sofitech.hmm.migration.service.VendorFormat;
import vn.sofitech.hmm.migration.web.MigrationDtos.*;
import vn.sofitech.hmm.shared.ApiException;

import java.io.IOException;
import java.util.List;

/** Chuyển đổi dữ liệu khách hàng từ POS cũ (FR-58 – FR-60). Chỉ Quản trị (cấu hình trong SecurityConfig). */
@RestController
@RequestMapping("/api/portal/migration")
public class MigrationController {

    private final CustomerImportService importer;

    public MigrationController(CustomerImportService importer) {
        this.importer = importer;
    }

    @GetMapping("/vendors")
    public List<VendorView> vendors() {
        return VendorFormat.ALL.stream().map(v -> new VendorView(v.code(), v.label(), String.valueOf(v.delimiter()), v.dobPattern(),
                v.columns())).toList();
    }

    @PostMapping("/customers")
    public ImportResult importCustomers(@RequestParam long storeId, @RequestParam String vendor, @RequestParam("file") MultipartFile file,
                                        @AuthenticationPrincipal Jwt jwt) throws IOException {
        if (file.isEmpty()) throw ApiException.badRequest("EMPTY", "Chưa chọn file");
        String name = file.getOriginalFilename() == null ? "upload.csv" : file.getOriginalFilename();
        return importer.importCustomers(storeId, vendor, name, file.getBytes(), CurrentUser.of(jwt).id());
    }

    @GetMapping("/batches")
    public List<BatchView> batches() {
        return importer.batches();
    }

    @GetMapping("/batches/{id}/errors")
    public List<ErrorView> errors(@PathVariable long id) {
        return importer.errorsOf(id);
    }
}
