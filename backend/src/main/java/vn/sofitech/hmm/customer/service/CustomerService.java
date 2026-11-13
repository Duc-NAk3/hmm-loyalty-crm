package vn.sofitech.hmm.customer.service;

import vn.sofitech.hmm.customer.api.CustomerInfo;
import vn.sofitech.hmm.customer.api.NewCustomer;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sofitech.hmm.customer.CustomerFacade;
import vn.sofitech.hmm.customer.domain.Consent;
import vn.sofitech.hmm.customer.repository.ConsentRepository;
import vn.sofitech.hmm.customer.domain.Customer;
import vn.sofitech.hmm.customer.repository.CustomerRepository;
import vn.sofitech.hmm.shared.ApiException;
import vn.sofitech.hmm.shared.SmsRequestedEvent;
import vn.sofitech.hmm.shared.Texts;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Hồ sơ thành viên: tra cứu, đăng ký, xác thực SĐT, consent (FR-18, FR-19, FR-20). */
@Service
@Transactional
public class CustomerService implements CustomerFacade {

    private final CustomerRepository customers;
    private final ConsentRepository consents;
    private final ApplicationEventPublisher events;

    public CustomerService(CustomerRepository customers, ConsentRepository consents, ApplicationEventPublisher events) {
        this.customers = customers;
        this.consents = consents;
        this.events = events;
    }

    static CustomerInfo toInfo(Customer c) {
        return new CustomerInfo(c.getId(), c.getMemberCode(), c.getPhone(), c.getFullName(), c.getStatus().name(), c.getTierCode(),
                c.getPointBalance(), c.getSpend12m(), c.isRedeemLocked(), c.isEmployee(), c.getRegisteredStoreId(), c.getCreatedAt());
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(Texts.VN);
    }

    private Customer locked(long id) {
        return customers.findForUpdate(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy khách hàng"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerInfo> findById(long customerId) {
        return customers.findById(customerId).map(CustomerService::toInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerInfo> lookup(String query) {
        if (Texts.isBlank(query)) return Optional.empty();
        String q = query.trim().toUpperCase();
        if (q.startsWith("HM")) return customers.findByMemberCodeAndStatusNot(q, Customer.Status.MERGED).map(CustomerService::toInfo);
        String phone = Texts.normalizePhone(q);
        if (phone == null) throw ApiException.badRequest("BAD_PHONE", "SĐT không hợp lệ");
        return findActiveByPhone(phone);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerInfo> findActiveByPhone(String normalizedPhone) {
        return customers.findByPhoneAndStatusNot(normalizedPhone, Customer.Status.MERGED).map(CustomerService::toInfo);
    }

    @Override
    public long create(NewCustomer c) {
        String phone = Texts.normalizePhone(c.getPhone());
        if (phone == null) throw ApiException.badRequest("BAD_PHONE", "SĐT không hợp lệ");
        if (Texts.isBlank(c.getFullName())) throw ApiException.badRequest("NAME", "Họ tên là bắt buộc");
        if (customers.findByPhoneAndStatusNot(phone, Customer.Status.MERGED).isPresent())
            throw ApiException.conflict("PHONE_EXISTS", "SĐT đã là thành viên");
        String memberCode = "HM" + customers.nextMemberNumber();
        Customer saved = customers.save(new Customer(memberCode, phone, Texts.normalizeName(c.getFullName()), c.getGender(), c.getDateOfBirth(),
                Texts.isBlank(c.getEmail()) ? null : c.getEmail().trim(), c.getStoreId(),
                c.getStatus() == null ? Customer.Status.UNVERIFIED : Customer.Status.valueOf(c.getStatus()),
                c.getCreatedAt() == null ? now() : c.getCreatedAt()));
        return saved.getId();
    }

    @Override
    public CustomerInfo registerAtPos(String phone, String fullName, String gender, LocalDate dob, boolean marketingConsent,
                                      long storeId, long cashierId) {
        long id = create(new NewCustomer(phone, fullName, gender, dob, null, storeId, "UNVERIFIED", null));
        Customer c = customers.getReferenceById(id);
        consents.save(new Consent(c, Consent.Purpose.MEMBERSHIP, Consent.Status.GRANTED, "POS", cashierId, now()));
        if (marketingConsent) consents.save(new Consent(c, Consent.Purpose.MARKETING_SMS, Consent.Status.GRANTED, "POS", cashierId, now()));
        events.publishEvent(new SmsRequestedEvent("SMS_WELCOME", "customers", id, c.getPhone(),
                "Chao mung " + Texts.unaccent(c.getFullName()).toUpperCase() + " den voi Hoa Mai Mart! Ma thanh vien cua ban: "
                        + c.getMemberCode() + ". Tich diem moi lan mua sam."));
        return toInfo(c);
    }

    @Override
    public boolean markVerified(long customerId) {
        return locked(customerId).markVerified(now());
    }

    @Override
    public int adjustPointBalance(long customerId, int delta, boolean allowNegative) {
        return locked(customerId).adjustPoints(delta, allowNegative, now());
    }

    @Override
    public CustomerInfo addSpend(long customerId, long amount) {
        Customer c = locked(customerId);
        c.addSpend(amount, now());
        return toInfo(c);
    }

    @Override
    public void setSpend12m(long customerId, long spend) {
        locked(customerId).setSpend12m(spend, now());
    }

    @Override
    public void changeTier(long customerId, String tierCode) {
        locked(customerId).changeTier(tierCode, now());
    }

    @Override
    public void fillMissing(long customerId, LocalDate dob, String email, String gender) {
        locked(customerId).fillMissing(dob, Texts.isBlank(email) ? null : email, gender, now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerInfo> findAllForTierReview() {
        return customers.findByStatusNotIn(List.of(Customer.Status.MERGED, Customer.Status.ANONYMIZED)).stream()
                .map(CustomerService::toInfo).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByTier() {
        return customers.findByStatusNotIn(List.of(Customer.Status.MERGED)).stream()
                .collect(Collectors.groupingBy(Customer::getTierCode, Collectors.counting()));
    }
}
