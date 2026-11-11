package vn.sofitech.hmm.migration;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.sofitech.hmm.admin.CurrentUser;
import vn.sofitech.hmm.shared.ApiException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Chuyển đổi dữ liệu khách hàng từ POS cũ (FR-58 – FR-60). Chỉ Quản trị. */
@RestController
@RequestMapping("/api/portal/migration")
public class MigrationController {

    private final CustomerImportService importer;

    public MigrationController(CustomerImportService importer) {
        this.importer = importer;
    }

    @GetMapping("/vendors")
    public List<Map<String, Object>> vendors() {
        return CustomerImportService.VENDORS.stream().map(v -> Map.<String, Object>of(
                "code", v.code(), "label", v.label(), "delimiter", String.valueOf(v.delimiter()), "dobPattern", v.dobPattern(),
                "columns", List.of(v.ref(), v.name(), v.phone(), v.dob(), v.email(), v.gender()))).toList();
    }

    @PostMapping("/customers")
    public Map<String, Object> importCustomers(@RequestParam long storeId, @RequestParam String vendor,
                                               @RequestParam("file") MultipartFile file, @AuthenticationPrincipal Jwt jwt) throws IOException {
        if (file.isEmpty()) throw ApiException.badRequest("EMPTY", "Chưa chọn file");
        String name = file.getOriginalFilename() == null ? "upload.csv" : file.getOriginalFilename();
        return importer.importCustomers(storeId, vendor, name, file.getBytes(), CurrentUser.of(jwt).id());
    }

    @GetMapping("/batches")
    public List<Map<String, Object>> batches() {
        return importer.batches();
    }

    @GetMapping("/batches/{id}/errors")
    public List<Map<String, Object>> errors(@PathVariable long id) {
        return importer.errors(id);
    }
}
