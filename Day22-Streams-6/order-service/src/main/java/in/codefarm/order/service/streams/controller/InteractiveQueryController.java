package in.codefarm.order.service.streams.controller;

import in.codefarm.order.service.streams.store.StoreQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API for Interactive Queries: read customer totals and category counts from state stores.
 */
@RestController
@RequestMapping("/api/store")
public class InteractiveQueryController {

	private final StoreQueryService storeQueryService;

	public InteractiveQueryController(StoreQueryService storeQueryService) {
		this.storeQueryService = storeQueryService;
	}

	@GetMapping("/customer-totals")
	public ResponseEntity<Map<String, Object>> getCustomerTotals(@RequestParam(required = false) String key) {
		if (!storeQueryService.isStoreReady()) {
			return ResponseEntity.status(503).body(Map.of(
					"status", "store_not_ready",
					"message", "Kafka Streams is not in RUNNING state. Wait for the application to be ready."
			));
		}
		if (key != null && !key.isBlank()) {
			Double total = storeQueryService.getCustomerTotal(key.trim());
			return ResponseEntity.ok(Map.of(
					"source", "state_store",
					"storeName", "customer-totals-store",
					"key", key.trim(),
					"total", total != null ? total : 0.0
			));
		}
		Map<String, Double> all = storeQueryService.getAllCustomerTotals();
		List<Map<String, Object>> entries = all.entrySet().stream()
				.map(e -> Map.<String, Object>of("key", e.getKey(), "total", e.getValue()))
				.collect(Collectors.toList());
		return ResponseEntity.ok(Map.of(
				"source", "state_store",
				"storeName", "customer-totals-store",
				"entries", entries
		));
	}

	@GetMapping("/category-counts")
	public ResponseEntity<Map<String, Object>> getCategoryCounts(@RequestParam(required = false) String key) {
		if (!storeQueryService.isStoreReady()) {
			return ResponseEntity.status(503).body(Map.of(
					"status", "store_not_ready",
					"message", "Kafka Streams is not in RUNNING state. Wait for the application to be ready."
			));
		}
		if (key != null && !key.isBlank()) {
			Long count = storeQueryService.getCategoryCount(key.trim());
			return ResponseEntity.ok(Map.of(
					"source", "state_store",
					"storeName", "orders-per-category-store",
					"key", key.trim(),
					"count", count != null ? count : 0L
			));
		}
		Map<String, Long> all = storeQueryService.getAllCategoryCounts();
		List<Map<String, Object>> entries = all.entrySet().stream()
				.map(e -> Map.<String, Object>of("key", e.getKey(), "count", e.getValue()))
				.collect(Collectors.toList());
		return ResponseEntity.ok(Map.of(
				"source", "state_store",
				"storeName", "orders-per-category-store",
				"entries", entries
		));
	}
}
