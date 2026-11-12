package vn.sofitech.hmm.loyalty.dto.tier;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.sofitech.hmm.loyalty.PointCalculator;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TiersResponse {

    private List<TierView> tiers;
    private PointCalculator.Policy policy;
}
