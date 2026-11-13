package vn.sofitech.hmm.report.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TierCountDto {

    private String tierCode;
    private String name;
    private long members;
}
