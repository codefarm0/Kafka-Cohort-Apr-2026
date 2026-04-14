package in.codefarm.order.service.as.producer.config;

import in.codefarm.order.service.as.producer.dto.OrderPlacedEvent;
import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
public class OrderPartitioner implements Partitioner {

    private final ObjectMapper objectMapper = new ObjectMapper();

//    public OrderPartitioner(ObjectMapper objectMapper) {
//        this.objectMapper = objectMapper;
//    }

    @Override
    public int partition(
        String topic,
        Object key,
        byte[] keyBytes,
        Object value,
        byte[] valueBytes,
        Cluster cluster
    ) {
        // Custom partitioning logic
        if (value instanceof String message) {

            var event = objectMapper.readValue(message, OrderPlacedEvent.class);
            // Partition by customer region or order type
            String customerId = event.customerId();
            
            // Example: Partition by customer ID prefix
            if (customerId.startsWith("US-")) {
                return 0; // US customers to partition 0
            } else if (customerId.startsWith("EU-")) {
                return 1; // EU customers to partition 1
            } else {
                return 2; // Others to partition 2
            }
        }
        
        // Fallback to default
        return 0;
    }
    
    @Override
    public void close() {
        // Cleanup if needed
    }
    
    @Override
    public void configure(Map<String, ?> configs) {
        // Configure partitioner if needed
    }
}