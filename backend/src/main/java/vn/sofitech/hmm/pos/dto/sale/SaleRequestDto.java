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
public class SaleRequestDto {

    private long terminalId;
    private long shiftId;
    private String receiptNo;
    private Long customerId;
    private List<LineInputDto> lines;
    private Long redemptionId;
    private long cashTendered;
}
