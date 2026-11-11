package vn.sofitech.hmm.loyalty;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nơi lưu mã OTP có thời hạn; mã OTP không lưu trong CSDL (ERD: point_redemptions.otp_verified_at).
 * Chạy local dùng bộ nhớ trong; profile docker dùng Redis như Architecture Proposal.
 */
public interface OtpStore {

    enum Result { OK, WRONG, EXPIRED, TOO_MANY_ATTEMPTS }

    void save(String key, String code, Duration ttl, int maxAttempts);

    Result verify(String key, String code);

    void remove(String key);

    @Component
    @ConditionalOnProperty(name = "app.otp-store", havingValue = "memory", matchIfMissing = true)
    class InMemory implements OtpStore {

        private record Entry(String code, Instant expiresAt, int attemptsLeft) {
        }

        private final Map<String, Entry> map = new ConcurrentHashMap<>();

        @Override
        public void save(String key, String code, Duration ttl, int maxAttempts) {
            map.put(key, new Entry(code, Instant.now().plus(ttl), maxAttempts));
        }

        @Override
        public Result verify(String key, String code) {
            Entry e = map.get(key);
            if (e == null || Instant.now().isAfter(e.expiresAt())) {
                map.remove(key);
                return Result.EXPIRED;
            }
            if (e.code().equals(code)) {
                map.remove(key);
                return Result.OK;
            }
            if (e.attemptsLeft() <= 1) {
                map.remove(key);
                return Result.TOO_MANY_ATTEMPTS;
            }
            map.put(key, new Entry(e.code(), e.expiresAt(), e.attemptsLeft() - 1));
            return Result.WRONG;
        }

        @Override
        public void remove(String key) {
            map.remove(key);
        }
    }

    @Component
    @ConditionalOnProperty(name = "app.otp-store", havingValue = "redis")
    class Redis implements OtpStore {

        private final StringRedisTemplate redis;

        public Redis(StringRedisTemplate redis) {
            this.redis = redis;
        }

        @Override
        public void save(String key, String code, Duration ttl, int maxAttempts) {
            String k = "otp:" + key;
            redis.delete(k);
            redis.opsForHash().putAll(k, Map.of("code", code, "attemptsLeft", String.valueOf(maxAttempts)));
            redis.expire(k, ttl);
        }

        @Override
        public Result verify(String key, String code) {
            String k = "otp:" + key;
            List<Object> v = redis.opsForHash().multiGet(k, List.of("code", "attemptsLeft"));
            if (v.get(0) == null) return Result.EXPIRED;
            if (code.equals(v.get(0))) {
                redis.delete(k);
                return Result.OK;
            }
            long left = redis.opsForHash().increment(k, "attemptsLeft", -1);
            if (left <= 0) {
                redis.delete(k);
                return Result.TOO_MANY_ATTEMPTS;
            }
            return Result.WRONG;
        }

        @Override
        public void remove(String key) {
            redis.delete("otp:" + key);
        }
    }
}
