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
public class Customer360View {

    private Profile profile;
    private Stats stats;
    private List<TxnRow> transactions;
    private List<LedgerRow> ledger;
    private List<ConsentRow> consents;
    private List<TierHistoryRow> tierHistory;
    private List<SourceRow> sources;
}
