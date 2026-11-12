package vn.sofitech.hmm.migration.dto.customerimport;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorView {

    private int rowNo;
    private String message;
    private String rawData;
}
