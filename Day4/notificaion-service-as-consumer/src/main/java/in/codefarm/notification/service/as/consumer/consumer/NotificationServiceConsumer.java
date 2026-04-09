package in.codefarm.notification.service.as.consumer.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationServiceConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(NotificationServiceConsumer.class);
    @KafkaListener(
        topics = {"orders2"},
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void notificationEventListener(ConsumerRecord<String, String> record) throws InterruptedException {
        log.info("Message received - {} ", record.value());
    }
}

