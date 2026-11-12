package vn.sofitech.hmm.migration.dto.vendor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorView {

    private String code;
    private String label;
    private String delimiter;
    private String dobPattern;
    private List<String> columns;
}
