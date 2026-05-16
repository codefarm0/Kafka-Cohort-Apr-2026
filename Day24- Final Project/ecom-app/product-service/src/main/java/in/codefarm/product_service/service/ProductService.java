package in.codefarm.product_service.service;

import in.codefarm.product_service.dto.DeductInventoryRequest;
import in.codefarm.product_service.entity.Product;
import in.codefarm.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Product catalog + stock (HLD Product Service).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProduct(String productId) {
        return productRepository.findByProductId(productId);
    }

    @Transactional
    public Product createOrUpdateProduct(String productId, String sku, String productName, String description,
                                         String categoryId, BigDecimal price, Integer quantity) {
        log.info("Upsert product: productId={}", productId);
        Optional<Product> existing = productRepository.findByProductId(productId);
        if (existing.isPresent()) {
            Product p = existing.get();
            p.setSku(sku);
            p.setProductName(productName);
            p.setDescription(description);
            p.setCategoryId(categoryId);
            p.setPrice(price);
            p.setAvailableQuantity(quantity);
            return productRepository.save(p);
        }
        Product product = Product.builder()
            .productId(productId)
            .sku(sku)
            .productName(productName)
            .description(description)
            .categoryId(categoryId)
            .price(price)
            .availableQuantity(quantity)
            .reservedQuantity(0)
            .build();
        return productRepository.save(product);
    }

    /**
     * Full replace of mutable fields (LLD PUT).
     */
    @Transactional
    public Product replaceProduct(String productId, String sku, String productName, String description,
                                  String categoryId, BigDecimal price, Integer availableQuantity,
                                  Integer reservedQuantity) {
        Product p = productRepository.findByProductId(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        p.setSku(sku);
        p.setProductName(productName);
        p.setDescription(description);
        p.setCategoryId(categoryId);
        p.setPrice(price);
        p.setAvailableQuantity(availableQuantity);
        if (reservedQuantity != null) {
            p.setReservedQuantity(reservedQuantity);
        }
        return productRepository.save(p);
    }

    @Transactional
    public void deleteProduct(String productId) {
        Product p = productRepository.findByProductId(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        productRepository.delete(p);
    }

    @Transactional
    public Product updateQuantity(String productId, Integer quantity) {
        Product product = productRepository.findByProductId(productId)
            .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        product.setAvailableQuantity(quantity);
        return productRepository.save(product);
    }

    @Transactional
    public void deductForOrder(DeductInventoryRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("No items to deduct for order: " + request.getOrderId());
        }
        log.info("Deducting stock for orderId={}", request.getOrderId());
        for (DeductInventoryRequest.Item item : request.getItems()) {
            String productId = item.getProductId();
            int qty = item.getQuantity() != null ? item.getQuantity() : 0;
            if (qty <= 0) {
                continue;
            }
            Product product = productRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
            int net = product.getNetAvailableQuantity();
            if (net < qty) {
                throw new IllegalStateException(
                    "Insufficient stock for product " + productId + ": need " + qty + ", net " + net);
            }
            int gross = product.getAvailableQuantity() + product.getReservedQuantity();
            product.setAvailableQuantity(gross - qty);
            productRepository.save(product);
            log.info("Deducted productId={}, qty={}, orderId={}", productId, qty, request.getOrderId());
        }
    }
}
