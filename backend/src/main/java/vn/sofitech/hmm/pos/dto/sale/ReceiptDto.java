package vn.sofitech.hmm.pos.dto.sale;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

/** Dữ liệu in hóa đơn (FR-05 ở mức màn hình xem trước): dòng hàng, thanh toán, điểm cộng, số dư, hạng. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptDto {

    private Long transactionId;
    private String receiptNo;
    private OffsetDateTime txnTime;
    private long netAmount;
    private Long eligibleAmount;
    private Integer pointsEarned;
    private Integer pointsRedeemed;
    private String storeCode;
    private String storeName;
    private String storeAddress;
    private String terminalCode;
    private String cashierName;
    private String memberCode;
    private String memberName;
    private String memberPhone;
    private Integer pointBalance;
    private String tierName;
    private List<ReceiptLineDto> lines;
    private List<PaymentDto> payments;
    private Long cashTendered;
    private Long change;
    private String upgradedTo;
}
