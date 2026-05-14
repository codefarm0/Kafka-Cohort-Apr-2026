package in.codefarm.order.service.dashboard;

import tools.jackson.databind.ObjectMapper;
import in.codefarm.order.service.streams.store.StoreQueryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Day 6 demo UI. Simulation sends orders to Kafka; dashboard shows data from output topics and from state store (Interactive Queries).
 */
@Controller
public class DashboardController {

	private static final String[] CATEGORIES = { "electronics", "books", "home", "sports", "toys" };

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final DashboardEventService dashboardEventService;
	private final StoreQueryService storeQueryService;
	private final ObjectMapper objectMapper;
	private final String ordersTopic;

	public DashboardController(KafkaTemplate<String, String> kafkaTemplate,
			DashboardEventService dashboardEventService,
			StoreQueryService storeQueryService,
			ObjectMapper objectMapper,
			@Value("${app.kafka.input-topic:orders}") String ordersTopic) {
		this.kafkaTemplate = kafkaTemplate;
		this.dashboardEventService = dashboardEventService;
		this.storeQueryService = storeQueryService;
		this.objectMapper = objectMapper;
		this.ordersTopic = ordersTopic;
	}

	@GetMapping("/")
	public String dashboard(Model model) {
		model.addAttribute("recentCustomerTotals", dashboardEventService.getRecentCustomerTotals());
		model.addAttribute("recentCategoryCounts", dashboardEventService.getRecentCategoryCounts());
		model.addAttribute("alerts", dashboardEventService.getAlerts());

		// Interactive Queries: current state from state stores (when streams is RUNNING)
		boolean storeReady = storeQueryService.isStoreReady();
		model.addAttribute("storeReady", storeReady);
		List<Map.Entry<String, Double>> customerTotalsFromStore = new ArrayList<>();
		List<Map.Entry<String, Long>> categoryCountsFromStore = new ArrayList<>();
		if (storeReady) {
			storeQueryService.getAllCustomerTotals().entrySet().stream()
					.sorted((a, b) -> a.getKey().compareTo(b.getKey()))
					.forEach(customerTotalsFromStore::add);
			storeQueryService.getAllCategoryCounts().entrySet().stream()
					.sorted((a, b) -> a.getKey().compareTo(b.getKey()))
					.forEach(categoryCountsFromStore::add);
		}
		model.addAttribute("customerTotalsFromStore", customerTotalsFromStore);
		model.addAttribute("categoryCountsFromStore", categoryCountsFromStore);

		return "dashboard";
	}

	@PostMapping("/orders")
	public String sendOrder(
			@RequestParam(required = false) String customerId,
			@RequestParam(required = false) String totalAmount,
			@RequestParam(required = false) String categoryId,
			RedirectAttributes redirectAttributes) {
		String customer = (customerId != null && !customerId.isBlank()) ? customerId : "customer-default";
		double amount = parseAmount(totalAmount, 50.0);
		String category = (categoryId != null && !categoryId.isBlank()) ? categoryId : "default";
		String value = toOrderJson(customer, amount, category);
		String key = "order-" + System.currentTimeMillis();
		kafkaTemplate.send(ordersTopic, key, value);
		redirectAttributes.addFlashAttribute("success", "Order submitted (customer=" + customer + ", total=" + amount + ", category=" + category + "). Results in dashboard and state store after processing.");
		return "redirect:/";
	}

	@PostMapping("/orders/bulk")
	public String sendOrdersBulk(
			@RequestParam(name = "count", defaultValue = "10") int count,
			@RequestParam(required = false) String customerPrefix,
			RedirectAttributes redirectAttributes) {
		if (count < 1) count = 1;
		if (count > 500) count = 500;
		String prefix = (customerPrefix != null && !customerPrefix.isBlank()) ? customerPrefix : "customer";
		for (int i = 0; i < count; i++) {
			String customer = prefix + "-" + (i % 10);
			double amount = 10.0 + (i % 5) * 25.0;
			String category = CATEGORIES[i % CATEGORIES.length];
			String value = toOrderJson(customer, amount, category);
			String key = "order-" + System.currentTimeMillis() + "-" + i;
			kafkaTemplate.send(ordersTopic, key, value);
		}
		redirectAttributes.addFlashAttribute("success", count + " orders submitted. Totals and counts will appear in dashboard and state store after processing.");
		return "redirect:/";
	}

	private String toOrderJson(String customerId, double totalAmount, String categoryId) {
		try {
			return objectMapper.writeValueAsString(Map.of(
					"customerId", customerId,
					"totalAmount", totalAmount,
					"categoryId", categoryId));
		} catch (Exception e) {
			return "{\"customerId\":\"" + customerId + "\",\"totalAmount\":" + totalAmount + ",\"categoryId\":\"" + categoryId + "\"}";
		}
	}

	private static double parseAmount(String s, double defaultVal) {
		if (s == null || s.isBlank()) return defaultVal;
		try {
			return Double.parseDouble(s.trim());
		} catch (NumberFormatException e) {
			return defaultVal;
		}
	}
}
