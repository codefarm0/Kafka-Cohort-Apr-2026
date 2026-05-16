package in.codefarm.order_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HTTP client for Product Service (catalog + deduct API).
 */
@Service
@Slf4j
public class InventoryServiceClient {

    private final RestTemplate restTemplate;
    private final String productServiceUrl;

    public InventoryServiceClient(
            RestTemplate restTemplate,
            @Value("${product.service.url:${inventory.service.url:http://localhost:8082}}") String productServiceUrl) {
        this.restTemplate = restTemplate;
        this.productServiceUrl = productServiceUrl;
    }
    
    /**
     * Get all products from inventory service.
     * Returns empty list if inventory service is unavailable.
     */
    public List<Product> getAllProducts() {
        try {
            String url = productServiceUrl + "/api/products";
            log.info("Fetching products from product service: {}", url);
            
            ResponseEntity<List<ProductResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ProductResponse>>() {}
            );
            
            List<Product> products = response.getBody().stream()
                .map(p -> new Product(
                    p.getProductId(),
                    p.getProductName(),
                    p.getPrice().doubleValue()
                ))
                .collect(Collectors.toList());
            
            log.info("Fetched {} products from inventory service", products.size());
            return products;
            
        } catch (RestClientException e) {
            log.warn("Failed to fetch products from product service: {}. Using empty list.", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Add a product to inventory service.
     */
    public Product addProduct(String productId, String productName, String description, 
                             BigDecimal price, Integer quantity) {
        try {
            String url = productServiceUrl + "/api/products";
            log.info("Adding product to product service: productId={}", productId);

            Map<String, Object> request = new HashMap<>();
            request.put("productId", productId);
            request.put("productName", productName);
            request.put("description", description);
            request.put("price", price);
            request.put("quantity", quantity);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ProductResponse response = restTemplate.postForObject(url, new HttpEntity<>(request, headers), ProductResponse.class);
            
            if (response != null) {
                log.info("Successfully added product: productId={}", productId);
                return new Product(response.getProductId(), response.getProductName(), 
                    response.getPrice().doubleValue());
            }
            
            throw new RuntimeException("Failed to add product: null response");
            
        } catch (RestClientException e) {
            log.error("Failed to add product to inventory service: productId={}", productId, e);
            throw new RuntimeException("Failed to add product: " + e.getMessage(), e);
        }
    }

    /**
     * Deduct inventory after successful payment (product/inventory service).
     */
    public void deductInventory(String orderId, List<DeductItem> items) {
        if (items == null || items.isEmpty()) {
            log.warn("deductInventory called with no items orderId={}", orderId);
            return;
        }
        String url = productServiceUrl + "/api/inventory/deduct";
        Map<String, Object> body = new HashMap<>();
        body.put("orderId", orderId);
        body.put("items", items.stream()
            .map(i -> Map.of("productId", i.productId(), "quantity", i.quantity()))
            .toList());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Void.class);
            log.info("Inventory deducted orderId={}", orderId);
        } catch (RestClientException e) {
            log.error("deductInventory failed orderId={}", orderId, e);
            throw new RuntimeException("Inventory deduct failed: " + e.getMessage(), e);
        }
    }

    public record DeductItem(String productId, int quantity) {}

    /**
     * Product data class for order service.
     */
    public static class Product {
        private String productId;
        private String productName;
        private double price;
        
        public Product(String productId, String productName, double price) {
            this.productId = productId;
            this.productName = productName;
            this.price = price;
        }
        
        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public double getPrice() { return price; }
    }
    
    /**
     * Product response from product service.
     */
    private static class ProductResponse {
        private String productId;
        private String productName;
        private String description;
        private BigDecimal price;
        private Integer availableQuantity;
        private Integer reservedQuantity;
        
        // Getters
        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public String getDescription() { return description; }
        public BigDecimal getPrice() { return price; }
        public Integer getAvailableQuantity() { return availableQuantity; }
        public Integer getReservedQuantity() { return reservedQuantity; }
        
        // Setters
        public void setProductId(String productId) { this.productId = productId; }
        public void setProductName(String productName) { this.productName = productName; }
        public void setDescription(String description) { this.description = description; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }
        public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }
    }
    
}

