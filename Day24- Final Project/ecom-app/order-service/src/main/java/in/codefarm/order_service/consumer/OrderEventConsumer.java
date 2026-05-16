package in.codefarm.order_service.consumer;

import in.codefarm.order_service.service.InventoryServiceClient;
import in.codefarm.order_service.service.OrderService;
import in.codefarm.order_service.service.RedisIdempotencyService;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Consumes payments and deliveries only (HLD saga).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;
    private final RedisIdempotencyService idempotencyService;

    @KafkaListener(
        topics = "payments",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentEvents(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        String eventJson = consumerRecord.value();
        String eventType = null;
        String eventId = null;
        String orderId = null;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> eventMap = objectMapper.readValue(eventJson, Map.class);
            eventType = (String) eventMap.get("eventType");
            eventId = (String) eventMap.get("eventId");

            Object dataObj = eventMap.get("data");
            Map<String, Object> dataMap = dataObj != null
                ? objectMapper.convertValue(dataObj, Map.class) : Map.of();
            orderId = (String) dataMap.get("orderId");

            log.info("Payment event: type={}, id={}, orderId={}", eventType, eventId, orderId);

            if (orderId == null || eventId == null) {
                acknowledgment.acknowledge();
                return;
            }

            if ("com.ecommerce.payment.processed".equals(eventType)) {
                String idemKey = "idempotency:order:" + orderId + ":payment:" + eventId;
                if (!idempotencyService.claimOnce(idemKey)) {
                    acknowledgment.acknowledge();
                    return;
                }
                try {
                    List<InventoryServiceClient.DeductItem> items = parseDeductItems(dataMap);
                    orderService.onPaymentProcessed(orderId, eventId, items);
                } catch (Exception ex) {
                    idempotencyService.forget("idempotency:order:" + orderId + ":payment:" + eventId);
                    throw ex;
                }
            } else if ("com.ecommerce.payment.failed".equals(eventType)) {
                String idemKey = "idempotency:order:" + orderId + ":paymentFailed:" + eventId;
                if (!idempotencyService.claimOnce(idemKey)) {
                    acknowledgment.acknowledge();
                    return;
                }
                orderService.onPaymentFailed(orderId);
            } else {
                log.debug("Ignoring payment topic event type: {}", eventType);
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing payment event: type={}, orderId={}", eventType, orderId, e);
            throw new RuntimeException("Failed to process payment event: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<InventoryServiceClient.DeductItem> parseDeductItems(Map<String, Object> dataMap) {
        List<InventoryServiceClient.DeductItem> out = new ArrayList<>();
        Object itemsObj = dataMap.get("items");
        if (itemsObj instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> m) {
                    String pid = String.valueOf(m.get("productId"));
                    Object q = m.get("quantity");
                    int qty = q instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(q));
                    if (pid != null && !"null".equals(pid) && qty > 0) {
                        out.add(new InventoryServiceClient.DeductItem(pid, qty));
                    }
                }
            }
        }
        return out;
    }

    @KafkaListener(
        topics = "deliveries",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDeliveryEvents(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        String eventJson = consumerRecord.value();
        String eventType = null;
        String eventId = null;
        String orderId = null;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> eventMap = objectMapper.readValue(eventJson, Map.class);
            eventType = (String) eventMap.get("eventType");
            eventId = (String) eventMap.get("eventId");
            Object dataObj = eventMap.get("data");
            Map<String, Object> dataMap = dataObj != null
                ? objectMapper.convertValue(dataObj, Map.class) : Map.of();
            orderId = (String) dataMap.get("orderId");
            String status = dataMap.get("status") != null
                ? String.valueOf(dataMap.get("status")).toUpperCase() : null;

            log.info("Delivery event: type={}, id={}, orderId={}, status={}", eventType, eventId, orderId, status);

            if (orderId == null || eventId == null) {
                acknowledgment.acknowledge();
                return;
            }

            boolean delivered = "DELIVERED".equals(status)
                || "com.ecommerce.delivery.delivered".equals(eventType);
            boolean shipped = !delivered && ("SHIPPED".equals(status)
                || "com.ecommerce.delivery.shipped".equals(eventType));

            if (!shipped && !delivered) {
                acknowledgment.acknowledge();
                return;
            }

            String idemKey = "idempotency:order:" + orderId + ":delivery:" + eventId;
            if (!idempotencyService.claimOnce(idemKey)) {
                acknowledgment.acknowledge();
                return;
            }

            if (delivered) {
                orderService.onDeliveryDelivered(orderId);
            } else {
                orderService.onDeliveryShipped(orderId);
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing delivery event: orderId={}", orderId, e);
            throw new RuntimeException("Failed to process delivery event: " + e.getMessage(), e);
        }
    }
}
