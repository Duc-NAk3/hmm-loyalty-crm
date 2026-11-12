package vn.sofitech.hmm.pos.dto.shift;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShiftView {

    private Long shiftId;
    private Long terminalId;
    private Long cashierId;
    private String cashierName;
    private OffsetDateTime openedAt;
    private long openingCash;
    private long receiptCount;
    private long cashSales;
}
