package vn.sofitech.hmm.customer.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.Texts;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Hồ sơ thành viên chuẩn – golden record (customer.customers).
 * Số dư điểm, chi tiêu 12 tháng và hạng do module loyalty cập nhật qua CustomerFacade.
 */
@Entity
@Table(schema = "customer", name = "customers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer {

    /** BRL-18: Chưa xác thực → Đã xác thực sau OTP đúng lần đầu. */
    public enum Status { UNVERIFIED, VERIFIED, LOCKED, MERGED, ANONYMIZED }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "customer_seq")
    @SequenceGenerator(name = "customer_seq", schema = "customer", sequenceName = "customers_customer_id_seq", allocationSize = 50)
    @Column(name = "customer_id")
    private Long id;

    @Column(name = "member_code", nullable = false, unique = true, updatable = false)
    private String memberCode;

    @Column(nullable = false)
    private String phone;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "name_search")
    private String nameSearch;

    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String email;

    @Column(name = "province_code")
    private String provinceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "tier_code", nullable = false)
    private String tierCode;

    @Column(name = "point_balance", nullable = false)
    private int pointBalance;

    @Column(name = "spend_12m", nullable = false)
    private long spend12m;

    @Column(name = "is_employee", nullable = false)
    private boolean employee;

    @Column(name = "redeem_locked", nullable = false)
    private boolean redeemLocked;

    @Column(name = "registered_store_id")
    private Long registeredStoreId;

    @Column(name = "merged_into_id")
    private Long mergedIntoId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public Customer(String memberCode, String phone, String fullName, String gender, LocalDate dateOfBirth, String email,
                    Long registeredStoreId, Status status, OffsetDateTime createdAt) {
        this.memberCode = memberCode;
        this.phone = phone;
        this.fullName = fullName;
        this.nameSearch = Texts.unaccent(fullName);
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.email = email;
        this.registeredStoreId = registeredStoreId;
        this.status = status;
        this.tierCode = "MEMBER";
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    /** OTP thành công lần đầu → Đã xác thực (FR-20). Trả về true nếu vừa chuyển trạng thái. */
    public boolean markVerified(OffsetDateTime at) {
        if (status != Status.UNVERIFIED) return false;
        status = Status.VERIFIED;
        verifiedAt = at;
        touch(at);
        return true;
    }

    /** Cộng hoặc trừ điểm khả dụng; trừ quá số dư thì từ chối (trừ khi cho phép âm – trả hàng, BRL-07). */
    public int adjustPoints(int delta, boolean allowNegative, OffsetDateTime at) {
        if (!allowNegative && delta < 0 && pointBalance + delta < 0)
            throw ApiException.conflict("INSUFFICIENT_POINTS", "Số dư điểm không đủ");
        pointBalance += delta;
        touch(at);
        return pointBalance;
    }

    public long addSpend(long amount, OffsetDateTime at) {
        spend12m += amount;
        touch(at);
        return spend12m;
    }

    public void setSpend12m(long spend, OffsetDateTime at) {
        spend12m = spend;
        touch(at);
    }

    public void changeTier(String tierCode, OffsetDateTime at) {
        this.tierCode = tierCode;
        touch(at);
    }

    /** Gộp dữ liệu chuyển đổi: chỉ bổ sung thông tin còn trống, giữ nguyên thông tin đã có. */
    public void fillMissing(LocalDate dob, String email, String gender, OffsetDateTime at) {
        if (dateOfBirth == null) dateOfBirth = dob;
        if (this.email == null) this.email = email;
        if (this.gender == null) this.gender = gender;
        touch(at);
    }

    public void lockRedemption(boolean locked) {
        this.redeemLocked = locked;
    }

    public boolean isActive() {
        return status == Status.UNVERIFIED || status == Status.VERIFIED;
    }

    private void touch(OffsetDateTime at) {
        updatedAt = at;
    }
}
