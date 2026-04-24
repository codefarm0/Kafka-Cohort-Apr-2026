package in.codefarm.order.service.as.producer.service;

import in.codefarm.order.service.as.producer.dto.OrderRequest;
import in.codefarm.order.service.as.producer.dto.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;

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
        kafkaTemplate.send("orders", event.orderId(), objectMapper.writeValueAsString(event));

        log.info("=== create order: Message sent (no confirmation) for order {} ===", event.orderId());
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

