package in.codefarm.schema.avro.consumer;

import in.codefarm.schema.avro.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    @KafkaListener(
        topics = "orders-avro",
        containerFactory = "avroKafkaListenerContainerFactory",
        groupId = "order-avro-consumer-group"
    )
    public void consume(OrderPlacedEvent event) {
        log.info("Received OrderPlacedEvent (Avro) - OrderId: {}, CustomerId: {}, ProductId: {}, Quantity: {}, Amount: {}, Date: {},Discount: {}",
            event.getOrderId(),
            event.getCustomerId(),
            event.getProductId(),
            event.getQuantity(),
            event.getTotalAmount(),
            event.getOrderDate(),
        event.getDiscount());
        // Process the order event
        processOrder(event);
    }
    
    private void processOrder(OrderPlacedEvent event) {
        log.info("Processing order: {}", event.getOrderId());
        // Business logic here
    }
}

