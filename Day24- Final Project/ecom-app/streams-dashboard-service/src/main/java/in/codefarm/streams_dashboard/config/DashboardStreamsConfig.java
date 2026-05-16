package in.codefarm.streams_dashboard.config;

import in.codefarm.streams_dashboard.streams.DashboardMetricsProcessor;
import java.util.List;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;

/**
 * Merged KStream over orders, payments, deliveries; durable counters in {@link #METRICS_STORE_NAME}.
 */
@Configuration
@EnableKafkaStreams
public class DashboardStreamsConfig {

    public static final String METRICS_STORE_NAME = "dashboard-metrics";

    @Bean
    public KStream<String, String> dashboardStream(StreamsBuilder builder) {
        StoreBuilder<KeyValueStore<String, Long>> metricsStore = Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(METRICS_STORE_NAME),
            Serdes.String(),
            Serdes.Long()
        );
        builder.addStateStore(metricsStore);

        KStream<String, String> merged = builder.stream(
            List.of("orders", "payments", "deliveries"),
            Consumed.with(Serdes.String(), Serdes.String())
        );

        merged.process(DashboardMetricsProcessor::new, METRICS_STORE_NAME);
        return merged;
    }
}
