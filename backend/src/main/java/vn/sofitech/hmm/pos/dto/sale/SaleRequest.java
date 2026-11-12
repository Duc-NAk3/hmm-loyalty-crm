package vn.sofitech.hmm.pos.dto.sale;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SaleRequest {

    private long terminalId;
    private long shiftId;
    private String receiptNo;
    private Long customerId;
    private List<LineInput> lines;
    private Long redemptionId;
    private long cashTendered;
}
