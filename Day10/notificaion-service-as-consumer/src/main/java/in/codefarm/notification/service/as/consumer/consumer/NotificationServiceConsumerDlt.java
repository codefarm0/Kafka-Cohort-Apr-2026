package in.codefarm.notification.service.as.consumer.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

//@Component
public class NotificationServiceConsumerDlt {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceConsumerDlt.class);
    @KafkaListener(
            topics = "order_events_dlt",
            containerFactory = "kafkaListenerContainerFactory",
            groupId = "notification-service-dlt"
    )
    public void notificationEventListener(ConsumerRecord<String, String> record) throws InterruptedException {
        log.info("Message received in DLT- {} ", record.value());
        //save the event into DB for later analysis
    }
}

