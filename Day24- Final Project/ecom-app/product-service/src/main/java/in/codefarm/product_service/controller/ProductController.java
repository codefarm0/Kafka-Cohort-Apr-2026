package in.codefarm.product_service.controller;

import in.codefarm.product_service.entity.Product;
import in.codefarm.product_service.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product catalog REST (HLD 3.1).
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProduct(@PathVariable String productId) {
        return productService.getProduct(productId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody CreateProductRequest request) {
        Product p = productService.createOrUpdateProduct(
            request.getProductId(),
            request.getSku(),
            request.getProductName(),
            request.getDescription(),
            request.getCategoryId(),
            request.getPrice(),
            request.getQuantity()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(p);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Product> replaceProduct(
            @PathVariable String productId,
            @Valid @RequestBody ReplaceProductRequest request) {
        Product p = productService.replaceProduct(
            productId,
            request.getSku(),
            request.getProductName(),
            request.getDescription(),
            request.getCategoryId(),
            request.getPrice(),
            request.getAvailableQuantity(),
            request.getReservedQuantity()
        );
        return ResponseEntity.ok(p);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable String productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{productId}/quantity")
    public ResponseEntity<Product> updateQuantity(
            @PathVariable String productId,
            @RequestBody UpdateQuantityRequest request) {
        return ResponseEntity.ok(productService.updateQuantity(productId, request.getQuantity()));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateProductRequest {
        @NotBlank
        private String productId;
        private String sku;
        @NotBlank
        private String productName;
        private String description;
        private String categoryId;
        @NotNull
        private BigDecimal price;
        @NotNull
        private Integer quantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReplaceProductRequest {
        private String sku;
        @NotBlank
        private String productName;
        private String description;
        private String categoryId;
        @NotNull
        private BigDecimal price;
        @NotNull
        private Integer availableQuantity;
        private Integer reservedQuantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateQuantityRequest {
        private Integer quantity;
    }
}
