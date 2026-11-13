package vn.sofitech.hmm.loyalty.service;

import vn.sofitech.hmm.loyalty.api.RedemptionRequested;
import vn.sofitech.hmm.loyalty.api.RedemptionHeld;
import vn.sofitech.hmm.loyalty.api.RedemptionInfo;
import vn.sofitech.hmm.loyalty.api.EarnResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.sofitech.hmm.customer.CustomerFacade;
import vn.sofitech.hmm.loyalty.LoyaltyFacade;
import vn.sofitech.hmm.loyalty.PointCalculator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Cài đặt Facade của module loyalty: điều phối các service nội bộ. */
@Service
public class LoyaltyFacadeImpl implements LoyaltyFacade {

    private final LoyaltyConfigService config;
    private final RedemptionService redemptions;
    private final LedgerService ledger;
    private final TierService tiers;
    private final CustomerFacade customers;

    public LoyaltyFacadeImpl(LoyaltyConfigService config, RedemptionService redemptions, LedgerService ledger, TierService tiers,
                             CustomerFacade customers) {
        this.config = config;
        this.redemptions = redemptions;
        this.ledger = ledger;
        this.tiers = tiers;
        this.customers = customers;
    }

    @Override
    public PointCalculator.Policy currentPolicy() {
        return config.currentPolicy();
    }

    @Override
    public String tierName(String tierCode) {
        return tierCode == null ? null : config.tier(tierCode).name();
    }

    @Override
    public PointCalculator.RedeemQuote quote(long customerId, long redeemableAmount) {
        return redemptions.quote(customerId, redeemableAmount);
    }

    @Override
    public RedemptionRequested requestRedemption(long customerId, long terminalId, int points, long redeemableAmount) {
        return redemptions.request(customerId, terminalId, points, redeemableAmount);
    }

    @Override
    public RedemptionHeld verifyOtp(long redemptionId, long terminalId, String otp, long cashierId) {
        return redemptions.verifyOtp(redemptionId, terminalId, otp, cashierId);
    }

    @Override
    public RedemptionInfo cancelRedemption(long redemptionId, long terminalId, long cashierId) {
        return redemptions.cancel(redemptionId, terminalId, cashierId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public RedemptionInfo confirmRedemption(long redemptionId, long customerId, long terminalId, long transactionId, long cashierId) {
        return redemptions.confirm(redemptionId, customerId, terminalId, transactionId, cashierId);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public EarnResult earnForSale(long customerId, long transactionId, long eligibleAmount, long saleTotal, OffsetDateTime at, long cashierId) {
        PointCalculator.Policy policy = config.currentPolicy();
        PointCalculator.Tier tier = config.tier(customers.findById(customerId).orElseThrow().getTierCode()); // hạng tại thời điểm giao dịch
        int earned = PointCalculator.earnedPoints(eligibleAmount, policy, tier.multiplier(), BigDecimal.ONE);
        int balance = ledger.earn(customerId, transactionId, earned, at, policy, cashierId);
        PointCalculator.Tier after = tiers.afterSale(customerId, saleTotal);
        return new EarnResult(earned, balance, after.code(), after.name(), after.code().equals(tier.code()) ? null : after.name());
    }
}
