package vn.sofitech.hmm.pos.web;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** DTO của module pos (camelCase). */
public final class PosDtos {

    private PosDtos() {
    }

    public record ShiftView(Long shiftId, Long terminalId, Long cashierId, String cashierName, OffsetDateTime openedAt,
                            long openingCash, long receiptCount, long cashSales) {
    }

    public record OpenShiftRequest(long terminalId, long openingCash) {
    }

    public record CloseShiftRequest(long countedCash) {
    }

    public record CloseShiftResult(Long shiftId, long expectedCash, long countedCash, long difference) {
    }

    public record LineInput(long productId, BigDecimal quantity) {
    }

    public record SaleRequest(long terminalId, long shiftId, String receiptNo, Long customerId, List<LineInput> lines,
                              Long redemptionId, long cashTendered) {
    }

    public record ReceiptLineView(String sku, String name, String unit, BigDecimal quantity, long unitPrice, long lineAmount,
                                  boolean pointEligible) {
    }

    public record PaymentView(String method, long amount) {
    }

    /** Dữ liệu in hóa đơn (FR-05 ở mức màn hình xem trước): dòng hàng, thanh toán, điểm cộng, số dư, hạng. */
    public record ReceiptView(Long transactionId, String receiptNo, OffsetDateTime txnTime, long netAmount, Long eligibleAmount,
                              Integer pointsEarned, Integer pointsRedeemed, String storeCode, String storeName, String storeAddress,
                              String terminalCode, String cashierName, String memberCode, String memberName, String memberPhone,
                              Integer pointBalance, String tierName, List<ReceiptLineView> lines, List<PaymentView> payments,
                              Long cashTendered, Long change, String upgradedTo) {
    }

    public record QuoteRequest(long customerId, List<LineInput> lines) {
    }

    public record QuoteResponse(boolean allowed, int maxPoints, long maxAmount, String reason, long redeemableAmount) {
    }

    public record RedeemRequest(long customerId, long terminalId, int points, List<LineInput> lines) {
    }

    public record VerifyRequest(long terminalId, String otp) {
    }

    public record CancelRequest(long terminalId) {
    }
}
