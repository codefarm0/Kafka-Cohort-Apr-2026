package in.codefarm.notification.service.as.consumer.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

//@Component
public class NotificationServiceConsumer3sDelay {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceConsumer3sDelay.class);
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public NotificationServiceConsumer3sDelay(ObjectMapper objectMapper, KafkaTemplate<String, String> kafkaTemplate) {
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(
            topics = "order_events_retry_3s",
            containerFactory = "kafkaListenerContainerFactory",
            groupId = "notification-service-retry-3s"
    )
    public void notificationEventListener(ConsumerRecord<String, String> record) throws InterruptedException {

        Thread.sleep(3000);//delay of 3 sec

        log.info("Message received - {} ", record.value());
        OrderPlacedEvent placedEvent = objectMapper.readValue(record.value(), OrderPlacedEvent.class);
        try {
            sendNotication(placedEvent.customerId(), placedEvent.orderId());
        } catch (TransientDownstreamException ex) {
            log.warn("Transient failure for orderId={}, routing to DLT", placedEvent.orderDate(), ex);
            kafkaTemplate.send("order_events_dlt",
                    String.valueOf(placedEvent.orderId()), objectMapper.writeValueAsString(placedEvent));
        } catch (Exception ex) {
            log.warn("Nonretryable failure for orderId={}, routing to dlt", placedEvent.orderDate(), ex);
            kafkaTemplate.send("order_events_dlt",
                    String.valueOf(placedEvent.orderId()), objectMapper.writeValueAsString(placedEvent));
        }
    }

    private void sendNotication(String customerId, String orderId) {
        // Simulate an unreliable downstream system ~30% of the time
        if (Math.random() < 0.9) {
            log.error("Downstream system is slow/unavailable for order {}", orderId);
            throw new TransientDownstreamException("Downstream timeout");
        }
//         Simulate some processing time
        try {
            Thread.sleep(500); // 0.5s
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("sent notification for user {}", customerId);
    }
}

