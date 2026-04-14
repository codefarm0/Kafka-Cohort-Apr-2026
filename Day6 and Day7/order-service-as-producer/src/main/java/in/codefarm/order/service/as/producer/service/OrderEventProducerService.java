package in.codefarm.order.service.as.producer.service;

import in.codefarm.order.service.as.producer.dto.OrderRequest;
import in.codefarm.order.service.as.producer.dto.OrderPlacedEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class OrderEventProducerService {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducerService.class);
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;


    public OrderEventProducerService(
            KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public OrderPlacedEvent createOrder(OrderRequest request) {
        log.info("=== create order: Sending order event {} ===", request.customerId());
        var event = createEvent(request);
        // Send and don't wait - fire and forget
        try {
            SendResult<String, String> result = kafkaTemplate.send("orders-partitioner", event.orderId(), objectMapper.writeValueAsString(event)).get();

            ProducerRecord<String, String> producerRecord = result.getProducerRecord();
            var metadata = result.getRecordMetadata();
            log.info("Message sent synchronously - Topic: {}, Partition: {}, Offset: {}",
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        log.info("=== create order: Message sent (no confirmation) for order {} ===", event.orderId());
        return event;
    }

    public OrderPlacedEvent createOrderWithCallback(OrderRequest request) {
        log.info("=== create order: Sending order event {} ===", request.customerId());
        var event = createEvent(request);
        // Send and don't wait - fire and forget

        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send("orders1234", event.orderId(), objectMapper.writeValueAsString(event));

        // Handle success
        future.thenAccept(result -> {
            var metadata = result.getRecordMetadata();
            log.info("Message sent successfully - Topic: {}, Partition: {}, Offset: {}",
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset());
        });

        // Handle failure
        future.exceptionally(ex -> {
            log.error("Failed to send message: {}", ex.getMessage(), ex);
            // Could send to retry queue, DLT, or alert here
            return null;
        });
        log.info("=== create order - {}",event.orderId());
        return event;
    }

    public OrderPlacedEvent createOrderWithHeader(OrderRequest request) {
        log.info("=== create order: Sending order event {} ===", request.customerId());
        var event = createEvent(request);

        ProducerRecord<String, String> record = new ProducerRecord<>(
                "orders-partitioner",
                event.orderId(),
                objectMapper.writeValueAsString(event)
        );

        // Add headers
        record.headers().add("correlation-id", event.orderId().getBytes());
        record.headers().add("source", "order-service".getBytes());
        record.headers().add("timestamp", String.valueOf(System.currentTimeMillis()).getBytes());
        record.headers().add("event-version", "1.0".getBytes());


        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(record);

        // Handle success
        future.thenAccept(result -> {
            var metadata = result.getRecordMetadata();
//            log.info("Message sent successfully - Topic: {}, Partition: {}, Offset: {}",
//                    metadata.topic(),
//                    metadata.partition(),
//                    metadata.offset());
        });

        // Handle failure
        future.exceptionally(ex -> {
//            log.error("Failed to send message: {}", ex.getMessage(), ex);
            // Could send to retry queue, DLT, or alert here
            return null;
        });
        log.info("=== create order - {}",event.orderId());
        return event;
    }

    private OrderPlacedEvent createEvent(OrderRequest request) {
        return new OrderPlacedEvent(
                UUID.randomUUID().toString(),
                request.customerId(),
                request.productId(),
                request.quantity(),
                request.totalAmount(),
                LocalDateTime.now(),
                UUID.randomUUID().toString()
        );
    }
}

