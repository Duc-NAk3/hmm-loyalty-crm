package vn.sofitech.hmm.migration.dto.customerimport;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {

    private Long batchId;
    private int totalRows;
    private int created;
    private int merged;
    private int errorRows;
    private List<ErrorView> errors;
}
