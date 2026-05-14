package in.codefarm.order.service.streams.store;

import in.codefarm.order.service.streams.topology.Day6StatefulTopology;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Queries Kafka Streams state stores via Interactive Queries.
 * Day 6: customer-totals-store (KeyValueStore&lt;String, Double&gt;) and orders-per-category-store (KeyValueStore&lt;String, Long&gt;).
 */
@Service
public class StoreQueryService {

	private static final Logger log = LoggerFactory.getLogger(StoreQueryService.class);

	private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;

	public StoreQueryService(StreamsBuilderFactoryBean streamsBuilderFactoryBean) {
		this.streamsBuilderFactoryBean = streamsBuilderFactoryBean;
	}

	public boolean isStoreReady() {
		KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
		return kafkaStreams != null && kafkaStreams.state() == KafkaStreams.State.RUNNING;
	}

	/** Point lookup: customer total by key. */
	public Double getCustomerTotal(String key) {
		KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
		if (kafkaStreams == null || kafkaStreams.state() != KafkaStreams.State.RUNNING) {
			return null;
		}
		try {
			ReadOnlyKeyValueStore<String, Double> store = kafkaStreams.store(
					StoreQueryParameters.fromNameAndType(Day6StatefulTopology.CUSTOMER_TOTALS_STORE, QueryableStoreTypes.keyValueStore()));
			return store.get(key);
		} catch (Exception e) {
			log.warn("Failed to query customer-totals store key={}", key, e);
			return null;
		}
	}

	/** All entries from customer-totals store. */
	public Map<String, Double> getAllCustomerTotals() {
		Map<String, Double> result = new HashMap<>();
		KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
		if (kafkaStreams == null || kafkaStreams.state() != KafkaStreams.State.RUNNING) {
			return result;
		}
		try {
			ReadOnlyKeyValueStore<String, Double> store = kafkaStreams.store(
					StoreQueryParameters.fromNameAndType(Day6StatefulTopology.CUSTOMER_TOTALS_STORE, QueryableStoreTypes.keyValueStore()));
			try (KeyValueIterator<String, Double> it = store.all()) {
				while (it.hasNext()) {
					var next = it.next();
					if (next.value != null) {
						result.put(next.key, next.value);
					}
				}
			}
		} catch (Exception e) {
			log.warn("Failed to query customer-totals store (all)", e);
		}
		return result;
	}

	/** Point lookup: category count by key. */
	public Long getCategoryCount(String key) {
		KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
		if (kafkaStreams == null || kafkaStreams.state() != KafkaStreams.State.RUNNING) {
			return null;
		}
		try {
			ReadOnlyKeyValueStore<String, Long> store = kafkaStreams.store(
					StoreQueryParameters.fromNameAndType(Day6StatefulTopology.ORDERS_PER_CATEGORY_STORE, QueryableStoreTypes.keyValueStore()));
			return store.get(key);
		} catch (Exception e) {
			log.warn("Failed to query orders-per-category store key={}", key, e);
			return null;
		}
	}

	/** All entries from orders-per-category store. */
	public Map<String, Long> getAllCategoryCounts() {
		Map<String, Long> result = new HashMap<>();
		KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
		if (kafkaStreams == null || kafkaStreams.state() != KafkaStreams.State.RUNNING) {
			return result;
		}
		try {
			ReadOnlyKeyValueStore<String, Long> store = kafkaStreams.store(
					StoreQueryParameters.fromNameAndType(Day6StatefulTopology.ORDERS_PER_CATEGORY_STORE, QueryableStoreTypes.keyValueStore()));
			try (KeyValueIterator<String, Long> it = store.all()) {
				while (it.hasNext()) {
					var next = it.next();
					if (next.value != null) {
						result.put(next.key, next.value);
					}
				}
			}
		} catch (Exception e) {
			log.warn("Failed to query orders-per-category store (all)", e);
		}
		return result;
	}
}
