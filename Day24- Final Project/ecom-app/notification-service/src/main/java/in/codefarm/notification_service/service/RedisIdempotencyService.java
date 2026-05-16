package in.codefarm.notification_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisIdempotencyService {

    private final StringRedisTemplate redisTemplate;

    public boolean claimOnce(String key, Duration ttl) {
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
        return Boolean.TRUE.equals(ok);
    }

    public void forget(String key) {
        redisTemplate.delete(key);
    }
}
