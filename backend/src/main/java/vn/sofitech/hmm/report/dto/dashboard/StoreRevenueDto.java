package vn.sofitech.hmm.report.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StoreRevenueDto {

    private String storeCode;
    private String name;
    private long receipts;
    private long revenue;
    private long memberRevenue;
    private long memberReceipts;
}
