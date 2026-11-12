package vn.sofitech.hmm.pos.dto.redemption;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.sofitech.hmm.pos.dto.sale.LineInput;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RedeemRequest {

    private long customerId;
    private long terminalId;
    private int points;
    private List<LineInput> lines;
}
