package vn.sofitech.hmm.report.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardView {

    private LocalDate from;
    private LocalDate to;
    private Long storeId;
    private Kpi kpi;
    private List<TierCount> tiers;
    private List<StoreRevenue> byStore;
    private List<DayRevenue> byDay;
    private List<CategoryRevenue> categories;
}
