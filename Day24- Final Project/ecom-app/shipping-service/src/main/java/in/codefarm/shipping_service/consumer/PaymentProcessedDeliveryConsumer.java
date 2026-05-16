package in.codefarm.shipping_service.consumer;

import in.codefarm.shipping_service.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * On successful payment, create delivery row (AWAITING_SHIPMENT). No {@code deliveries} topic publish here.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentProcessedDeliveryConsumer {

    private final DeliveryService deliveryService;
    private final ObjectMapper objectMapper;

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

            if (!"com.ecommerce.payment.processed".equals(eventType)) {
                acknowledgment.acknowledge();
                return;
            }

            Object dataObj = eventMap.get("data");
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = dataObj != null
                ? objectMapper.convertValue(dataObj, Map.class) : Map.of();
            orderId = (String) dataMap.get("orderId");
            String customerId = (String) dataMap.get("customerId");
            String shippingAddress = (String) dataMap.get("shippingAddress");

            if (orderId == null || eventId == null) {
                log.warn("payment.processed missing orderId or eventId");
                acknowledgment.acknowledge();
                return;
            }

            log.info("Delivery: payment processed orderId={}, eventId={}", orderId, eventId);
            deliveryService.createAwaitingShipmentAfterPayment(orderId, customerId, shippingAddress, eventId);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Delivery payment consumer error orderId={}", orderId, e);
            throw new RuntimeException("Failed to process payment for delivery: " + e.getMessage(), e);
        }
    }
}
