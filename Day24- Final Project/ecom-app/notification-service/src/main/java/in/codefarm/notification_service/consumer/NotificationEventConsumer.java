package in.codefarm.notification_service.consumer;

import in.codefarm.notification_service.service.NotificationService;
import in.codefarm.notification_service.service.RedisIdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Map;

/**
 * HLD Phase 5: {@code orders}, {@code payments}, {@code deliveries} only. Redis idempotency per CloudEvent id.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final RedisIdempotencyService idempotency;

    @KafkaListener(topics = "orders", containerFactory = "kafkaListenerContainerFactory")
    public void handleOrderEvents(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        processWhenRelevant(consumerRecord, acknowledgment, "com.ecommerce.order.placed", (eventType, eventId, data) -> {
            String orderId = str(data.get("orderId"));
            String customerId = str(data.get("customerId"));
            notificationService.sendOrderPlacedNotification(orderId, customerId);
        });
    }

    @KafkaListener(topics = "payments", containerFactory = "kafkaListenerContainerFactory")
    public void handlePaymentEvents(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        processWhenRelevant(consumerRecord, acknowledgment, null, (eventType, eventId, data) -> {
            String orderId = str(data.get("orderId"));
            String customerId = str(data.get("customerId"));
            if ("com.ecommerce.payment.processed".equals(eventType)) {
                notificationService.sendPaymentProcessedNotification(orderId, customerId);
            } else if ("com.ecommerce.payment.failed".equals(eventType)) {
                notificationService.sendPaymentFailedNotification(orderId, customerId);
            }
        });
    }

    @KafkaListener(topics = "deliveries", containerFactory = "kafkaListenerContainerFactory")
    public void handleDeliveryEvents(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        processWhenRelevant(consumerRecord, acknowledgment, null, (eventType, eventId, data) -> {
            String orderId = str(data.get("orderId"));
            String customerId = str(data.get("customerId"));
            if ("com.ecommerce.delivery.delivered".equals(eventType)) {
                notificationService.sendDeliveryDeliveredNotification(orderId, customerId);
            } else if ("com.ecommerce.delivery.shipped".equals(eventType)) {
                notificationService.sendDeliveryShippedNotification(orderId, customerId);
            }
        });
    }

    @FunctionalInterface
    private interface EventHandler {
        void handle(String eventType, String eventId, Map<String, Object> data) throws Exception;
    }

    /**
     * @param singleType if non-null, only this eventType is processed (others ack immediately).
     *                   If null, handler must no-op for unknown types (no Redis claim for irrelvant events).
     */
    @SuppressWarnings("unchecked")
    private void processWhenRelevant(ConsumerRecord<String, String> record, Acknowledgment ack,
                                     String singleType, EventHandler handler) {
        String raw = record.value();
        String eventType = null;
        String eventId = null;
        try {
            Map<String, Object> eventMap = objectMapper.readValue(raw, Map.class);
            eventType = (String) eventMap.get("eventType");
            eventId = (String) eventMap.get("eventId");

            if (singleType != null && !singleType.equals(eventType)) {
                ack.acknowledge();
                return;
            }

            if (eventId == null || eventId.isBlank()) {
                log.warn("Skip notification event without eventId type={}", eventType);
                ack.acknowledge();
                return;
            }

            Object dataObj = eventMap.get("data");
            Map<String, Object> data = dataObj instanceof Map<?, ?> ? (Map<String, Object>) dataObj : Map.of();

            if (singleType == null && !isPaymentOrDeliveryNotificationEvent(eventType)) {
                ack.acknowledge();
                return;
            }

            String idemKey = "idempotency:notification:" + eventId;
            if (!idempotency.claimOnce(idemKey, Duration.ofHours(24))) {
                ack.acknowledge();
                return;
            }
            try {
                handler.handle(eventType, eventId, data);
            } catch (Exception ex) {
                idempotency.forget(idemKey);
                throw ex;
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Notification consumer error type={} eventId={}", eventType, eventId, e);
            throw new RuntimeException("Failed to process notification event: " + e.getMessage(), e);
        }
    }

    private static boolean isPaymentOrDeliveryNotificationEvent(String eventType) {
        return "com.ecommerce.payment.processed".equals(eventType)
            || "com.ecommerce.payment.failed".equals(eventType)
            || "com.ecommerce.delivery.delivered".equals(eventType)
            || "com.ecommerce.delivery.shipped".equals(eventType);
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
