package vn.sofitech.hmm.shared;

/**
 * Sự kiện "cần gửi SMS" (Observer pattern). Module nghiệp vụ chỉ phát sự kiện qua ApplicationEventPublisher;
 * {@link SmsOutbox} lắng nghe và ghi vào hộp thư đi trong cùng giao dịch.
 *
 * @param messageType   SMS_OTP / SMS_WELCOME / SMS_TIER ...
 * @param aggregateType loại đối tượng gốc, ví dụ "customers"
 */
public record SmsRequestedEvent(String messageType, String aggregateType, long aggregateId, String phone, String text) {
}
