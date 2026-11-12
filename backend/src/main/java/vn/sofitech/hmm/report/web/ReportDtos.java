package vn.sofitech.hmm.report.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/** DTO của module report (mô hình đọc – read model). */
public final class ReportDtos {

    private ReportDtos() {
    }

    // ---------------- Dashboard (FR-51) ----------------

    public record Kpi(long receipts, long revenue, long memberReceipts, long memberRevenue, long activeMembers, long pointsEarned,
                      long pointsRedeemed, double identificationRate, double memberRevenueShare, long avgBasket, double returningRate,
                      long totalMembers, long newMembers, long verifiedMembers) {
    }

    public record TierCount(String tierCode, String name, long members) {
    }

    public record StoreRevenue(String storeCode, String name, long receipts, long revenue, long memberRevenue, long memberReceipts) {
    }

    public record DayRevenue(LocalDate day, long memberRevenue, long guestRevenue, long receipts, long memberReceipts) {
    }

    public record CategoryRevenue(String name, long revenue) {
    }

    public record DashboardView(LocalDate from, LocalDate to, Long storeId, Kpi kpi, List<TierCount> tiers, List<StoreRevenue> byStore,
                                List<DayRevenue> byDay, List<CategoryRevenue> categories) {
    }

    // ---------------- Khách hàng, Customer 360 (FR-22) ----------------

    public record CustomerRow(Long customerId, String memberCode, String fullName, String phone, String tierCode, String tierName,
                              String status, int pointBalance, long spend12m, String registeredStore, OffsetDateTime createdAt) {
    }

    public record Profile(Long customerId, String memberCode, String fullName, String phone, String gender, LocalDate dateOfBirth,
                          String email, String status, OffsetDateTime verifiedAt, String tierCode, String tierName, BigDecimal multiplier,
                          int pointBalance, long spend12m, boolean redeemLocked, boolean employee, String registeredStore,
                          String registeredStoreName, OffsetDateTime createdAt, Long mergedIntoId, String nextTier,
                          Long nextThreshold, long thresholdSpend) {
    }

    public record Stats(long receipts, long lifetimeSpend, OffsetDateTime lastPurchase, long storesVisited, long pointsEarnedTotal,
                        long pointsRedeemedTotal, long ledgerSum, long expiringPoints, OffsetDateTime nextExpiry) {
    }

    public record TxnRow(Long transactionId, String receiptNo, OffsetDateTime txnTime, String storeCode, String storeName, String txnType,
                         long netAmount, Integer pointsEarned, Integer pointsRedeemed, String source, long lineCount) {
    }

    public record LedgerRow(Long ledgerId, OffsetDateTime createdAt, String entryType, int points, int balanceAfter, String receiptNo,
                            Long redemptionId, OffsetDateTime expiresAt) {
    }

    public record ConsentRow(String purpose, String status, String channel, String termsVersion, OffsetDateTime capturedAt, String capturedBy) {
    }

    public record TierHistoryRow(OffsetDateTime changedAt, String fromTier, String toTier, String reason) {
    }

    public record SourceRow(String sourceVendor, String sourceCustomerRef, Long batchId, OffsetDateTime importedAt, String storeCode) {
    }

    public record Customer360View(Profile profile, Stats stats, List<TxnRow> transactions, List<LedgerRow> ledger,
                                  List<ConsentRow> consents, List<TierHistoryRow> tierHistory, List<SourceRow> sources) {
    }

    public record LineRow(String sku, String name, String category, BigDecimal quantity, long unitPrice, long lineAmount, boolean pointEligible) {
    }

    public record PhoneView(String phone) {
    }

    public record AuditRow(Long auditId, OffsetDateTime createdAt, String username, String action, String entityType, String entityId,
                           String afterData) {
    }
}
