package vn.sofitech.hmm.report.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer360Dto {

    private ProfileDto profile;
    private StatsDto stats;
    private List<TxnRowDto> transactions;
    private List<LedgerRowDto> ledger;
    private List<ConsentRowDto> consents;
    private List<TierHistoryRowDto> tierHistory;
    private List<SourceRowDto> sources;
}
