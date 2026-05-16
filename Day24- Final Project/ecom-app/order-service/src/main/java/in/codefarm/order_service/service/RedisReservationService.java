package in.codefarm.order_service.service;

import in.codefarm.order_service.event.OrderPlacedEventData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis inventory reservation (1 minute TTL) per HLD 3.12.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisReservationService {

    private static final String KEY_PREFIX = "reserve:order:";
    private static final long TTL_SECONDS = 60;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void reserveOrder(String orderId, List<OrderPlacedEventData.OrderItemData> items) {
        String key = KEY_PREFIX + orderId;
        try {
            String json = objectMapper.writeValueAsString(items.stream()
                .map(i -> new ReserveItem(i.getProductId(), i.getQuantity()))
                .collect(Collectors.toList()));
            redisTemplate.opsForValue().set(key, json, TTL_SECONDS, TimeUnit.SECONDS);
            log.info("Redis reservation set: key={}, ttl={}s", key, TTL_SECONDS);
        } catch (Exception e) {
            log.error("Failed to set Redis reservation for orderId={}", orderId, e);
            throw new RuntimeException("Redis reservation failed", e);
        }
    }

    public void releaseReservation(String orderId) {
        String key = KEY_PREFIX + orderId;
        Boolean deleted = redisTemplate.delete(key);
        log.info("Redis reservation released: key={}, deleted={}", key, deleted);
    }

    private record ReserveItem(String productId, Integer quantity) {}
}
