package in.codefarm.order_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Consumer idempotency keys in Redis (HLD 3.12).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisIdempotencyService {

    private static final long DEFAULT_TTL_HOURS = 24;

    private final StringRedisTemplate redisTemplate;

    /**
     * @return true if this is the first time processing (key was set), false if duplicate.
     */
    public boolean claimOnce(String key) {
        return claimOnce(key, Duration.ofHours(DEFAULT_TTL_HOURS));
    }

    public boolean claimOnce(String key, Duration ttl) {
        Boolean set = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
        boolean first = Boolean.TRUE.equals(set);
        if (!first) {
            log.debug("Idempotency skip (already processed): {}", key);
        }
        return first;
    }

    /** Allow consumer retry after a failed attempt (e.g. inventory deduct transient failure). */
    public void forget(String key) {
        redisTemplate.delete(key);
    }
}
