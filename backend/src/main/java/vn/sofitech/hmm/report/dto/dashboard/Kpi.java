package vn.sofitech.hmm.report.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Kpi {

    private long receipts;
    private long revenue;
    private long memberReceipts;
    private long memberRevenue;
    private long activeMembers;
    private long pointsEarned;
    private long pointsRedeemed;
    private double identificationRate;
    private double memberRevenueShare;
    private long avgBasket;
    private double returningRate;
    private long totalMembers;
    private long newMembers;
    private long verifiedMembers;
}
