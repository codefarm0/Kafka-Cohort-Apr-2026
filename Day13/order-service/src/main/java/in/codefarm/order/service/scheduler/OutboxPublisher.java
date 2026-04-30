package in.codefarm.order.service.scheduler;

import in.codefarm.order.service.domain.OutboxEvent;
import in.codefarm.order.service.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 10000)
    public void publishNewEvents() {
        List<OutboxEvent> events =
                outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc("NEW");

        log.info("Got {} records", events.size());
        for (OutboxEvent event : events) {
            publishSingleEvent(event);
        }
    }

    @Transactional
    public void publishSingleEvent(OutboxEvent event) {
        try {
            kafkaTemplate.send("order_events",
                    String.valueOf(event.getAggregateId()),
                    event.getPayload()
            ).get(); // important

            event.setStatus("SENT");
            event.setSentAt(Instant.now());
            outboxEventRepository.save(event);

        } catch (Exception ex) {
            log.error("Failed for id={}", event.getId(), ex);
            // rollback only this event transaction
            throw new RuntimeException(ex);
        }
    }

}