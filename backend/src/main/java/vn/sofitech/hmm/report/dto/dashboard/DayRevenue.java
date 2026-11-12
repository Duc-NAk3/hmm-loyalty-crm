package vn.sofitech.hmm.report.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DayRevenue {

    private LocalDate day;
    private long memberRevenue;
    private long guestRevenue;
    private long receipts;
    private long memberReceipts;
}
