package in.codefarm.notification.service.as.consumer.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class NotificationServiceConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceConsumer.class);
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public NotificationServiceConsumer(ObjectMapper objectMapper, KafkaTemplate<String, String> kafkaTemplate) {
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

//    @KafkaListener(
//            topics = "orders",
//            containerFactory = "kafkaListenerContainerFactory",
//            groupId = "notification-service"
//    )
    public void notificationEventListener(ConsumerRecord<String, String> record) throws InterruptedException {

        log.info("Message received - {} ", record.value());
        OrderPlacedEvent placedEvent = objectMapper.readValue(record.value(), OrderPlacedEvent.class);
        try {
            sendNotication(placedEvent.customerId(), placedEvent.orderId());
        } catch (TransientDownstreamException ex) {
            log.warn("Transient failure for orderId={}, routing to retry-1s", placedEvent.orderDate(), ex);
            kafkaTemplate.send("order_events_retry_1s",
                    String.valueOf(placedEvent.orderId()), objectMapper.writeValueAsString(placedEvent));
        } catch (Exception ex) {
            log.warn("Non-retryable failure for orderId={}, routing to dlt", placedEvent.orderDate(), ex);
            kafkaTemplate.send("order_events_dlt",
                    String.valueOf(placedEvent.orderId()), objectMapper.writeValueAsString(placedEvent));
        }
    }








//orders, orders-retry-1000 , orders-retry-2000

    @KafkaListener(
            topics = "orders",
            containerFactory = "kafkaListenerContainerFactory",
            groupId = "notification-service-consumer"
    )
        @RetryableTopic(
            attempts = "3",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_DELAY_VALUE,
            retryTopicSuffix = "-retry",
            backOff = @BackOff(delay = 1000, multiplier = 2),
            include = {TransientDownstreamException.class}
//            exclude = {Exception.class}
    )
    public void notificationEventListenerWithAnnotation(ConsumerRecord<String, String> record) throws InterruptedException {
        log.info("Message received - {} ", record.value());
        OrderPlacedEvent placedEvent = objectMapper.readValue(record.value(), OrderPlacedEvent.class);
        sendNotication(placedEvent.customerId(), placedEvent.orderId());

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


    @DltHandler
    public void handleDlt(ConsumerRecord<String, String> record){
        log.info("msg received in DLT- {}, header - {}", record.value(), record.headers() );
        //save into DB

    }
}

