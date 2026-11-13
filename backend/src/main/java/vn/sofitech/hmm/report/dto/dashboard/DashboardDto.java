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
public class DashboardDto {

    private LocalDate from;
    private LocalDate to;
    private Long storeId;
    private KpiDto kpi;
    private List<TierCountDto> tiers;
    private List<StoreRevenueDto> byStore;
    private List<DayRevenueDto> byDay;
    private List<CategoryRevenueDto> categories;
}
