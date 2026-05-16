package in.codefarm.shipping_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisIdempotencyService {

    private final StringRedisTemplate redisTemplate;

    public boolean claimOnce(String key, Duration ttl) {
        Boolean set = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
        return Boolean.TRUE.equals(set);
    }

    public boolean claimOnce24h(String key) {
        return claimOnce(key, Duration.ofHours(24));
    }
}
