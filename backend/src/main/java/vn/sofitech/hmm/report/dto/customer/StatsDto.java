package vn.sofitech.hmm.report.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatsDto {

    private long receipts;
    private long lifetimeSpend;
    private OffsetDateTime lastPurchase;
    private long storesVisited;
    private long pointsEarnedTotal;
    private long pointsRedeemedTotal;
    private long ledgerSum;
    private long expiringPoints;
    private OffsetDateTime nextExpiry;
}
