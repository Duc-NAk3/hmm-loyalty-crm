package vn.sofitech.hmm.catalog.dto.product;

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

    private int created;
    private int updated;
    private List<String> errors;
}
