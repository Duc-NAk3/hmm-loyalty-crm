package vn.sofitech.hmm.pos.dto.shift;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpenShiftRequest {

    private long terminalId;
    private long openingCash;
}
