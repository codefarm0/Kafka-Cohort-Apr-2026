package in.codefarm.streams_dashboard.service;

import static in.codefarm.streams_dashboard.config.DashboardStreamsConfig.METRICS_STORE_NAME;

import in.codefarm.streams_dashboard.streams.DashboardMetricKeys;
import in.codefarm.streams_dashboard.web.DashboardMetricsResponse;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

@Service
public class DashboardMetricsService {

    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    public DashboardMetricsService(
            @Qualifier(KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_BUILDER_BEAN_NAME)
            StreamsBuilderFactoryBean streamsBuilderFactoryBean) {
        this.streamsBuilderFactoryBean = streamsBuilderFactoryBean;
    }

    public DashboardMetricsResponse snapshot() {
        KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
        if (kafkaStreams == null) {
            return DashboardMetricsResponse.empty("NOT_STARTED");
        }
        String state = kafkaStreams.state().name();
        if (kafkaStreams.state() != KafkaStreams.State.RUNNING) {
            return DashboardMetricsResponse.empty(state);
        }
        try {
            ReadOnlyKeyValueStore<String, Long> store = kafkaStreams.store(
                StoreQueryParameters.fromNameAndType(METRICS_STORE_NAME, QueryableStoreTypes.keyValueStore())
            );
            long revenueCents = get(store, DashboardMetricKeys.REVENUE_CENTS);
            return new DashboardMetricsResponse(
                get(store, DashboardMetricKeys.ORDERS_PLACED),
                get(store, DashboardMetricKeys.PAYMENTS_SUCCEEDED),
                get(store, DashboardMetricKeys.PAYMENTS_FAILED),
                get(store, DashboardMetricKeys.DELIVERIES_SHIPPED),
                get(store, DashboardMetricKeys.DELIVERIES_DELIVERED),
                revenueCents / 100.0,
                state
            );
        } catch (InvalidStateStoreException e) {
            return DashboardMetricsResponse.empty(state);
        }
    }

    private static long get(ReadOnlyKeyValueStore<String, Long> store, String key) {
        Long v = store.get(key);
        return v == null ? 0L : v;
    }
}
