package in.codefarm.order.service.as.producer.config;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import java.util.Map;

public class OrderPartitioner implements Partitioner {
    @Override
    public int partition(
            String topic,
            Object key,
            byte[] keyBytes,
            Object value,
            byte[] valueBytes,
            Cluster cluster) {
        int partitionCount = cluster.partitionCountForTopic(topic);
        String orderId = (String) key;
        return Math.abs(orderId.hashCode()) % partitionCount;
    }
    @Override
    public void close() {
        // No-op
    }
    @Override
    public void configure(Map<String, ?> map) {
        // No-op
    }
}