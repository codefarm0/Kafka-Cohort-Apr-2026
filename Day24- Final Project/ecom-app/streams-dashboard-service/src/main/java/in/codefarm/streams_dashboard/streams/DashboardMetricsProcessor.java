package in.codefarm.streams_dashboard.streams;

import static in.codefarm.streams_dashboard.config.DashboardStreamsConfig.METRICS_STORE_NAME;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses JSON ({@code eventType} + {@code data}), updates aggregate counters in the state store.
 */
public class DashboardMetricsProcessor implements Processor<String, String, Void, Void> {

    private static final Logger log = LoggerFactory.getLogger(DashboardMetricsProcessor.class);
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private KeyValueStore<String, Long> store;

    @Override
    public void init(ProcessorContext<Void, Void> context) {
        this.store = context.getStateStore(METRICS_STORE_NAME);
    }

    @Override
    public void process(Record<String, String> record) {
        if (record.value() == null || record.value().isBlank()) {
            return;
        }
        try {
            JsonNode root = MAPPER.readTree(record.value());
            String eventType = text(root, "eventType");
            if (eventType == null || eventType.isBlank()) {
                return;
            }
            JsonNode data = root.get("data");

            switch (eventType) {
                case "com.ecommerce.order.placed" -> increment(DashboardMetricKeys.ORDERS_PLACED, 1L);
                case "com.ecommerce.payment.processed" -> {
                    increment(DashboardMetricKeys.PAYMENTS_SUCCEEDED, 1L);
                    addRevenueFromData(data);
                }
                case "com.ecommerce.payment.failed" -> increment(DashboardMetricKeys.PAYMENTS_FAILED, 1L);
                case "com.ecommerce.delivery.shipped" -> increment(DashboardMetricKeys.DELIVERIES_SHIPPED, 1L);
                case "com.ecommerce.delivery.delivered" -> increment(DashboardMetricKeys.DELIVERIES_DELIVERED, 1L);
                default -> { /* ignore unknown types */ }
            }
        } catch (Exception e) {
            log.warn("Skipping malformed dashboard event: {}", e.getMessage());
        }
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.has(field)) {
            return null;
        }
        JsonNode n = node.get(field);
        return n.isNull() ? null : n.asText();
    }

    private void increment(String key, long delta) {
        Long cur = store.get(key);
        long next = (cur == null ? 0L : cur) + delta;
        store.put(key, next);
    }

    private void addRevenueFromData(JsonNode data) {
        if (data == null || data.isNull() || !data.has("amount")) {
            return;
        }
        JsonNode amountNode = data.get("amount");
        BigDecimal amount;
        if (amountNode.isNumber()) {
            amount = amountNode.decimalValue();
        } else {
            try {
                amount = new BigDecimal(amountNode.asText());
            } catch (NumberFormatException e) {
                log.debug("Could not parse payment amount: {}", amountNode);
                return;
            }
        }
        long cents = amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
        increment(DashboardMetricKeys.REVENUE_CENTS, cents);
    }
}
