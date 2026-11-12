package vn.sofitech.hmm.migration.web;

import java.time.OffsetDateTime;
import java.util.List;

/** DTO của module migration. */
public final class MigrationDtos {

    private MigrationDtos() {
    }

    public record VendorView(String code, String label, String delimiter, String dobPattern, List<String> columns) {
    }

    public record ErrorView(int rowNo, String message, String rawData) {
    }

    public record ImportResult(Long batchId, int totalRows, int created, int merged, int errorRows, List<ErrorView> errors) {
    }

    public record BatchView(Long batchId, String storeCode, String sourceVendor, String dataType, String status, Integer totalRows,
                            Integer errorRows, OffsetDateTime importedAt, String importedBy, long importedRows) {
    }
}
