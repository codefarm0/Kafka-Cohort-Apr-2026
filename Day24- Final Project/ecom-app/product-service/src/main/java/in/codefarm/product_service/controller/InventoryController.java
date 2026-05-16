package in.codefarm.product_service.controller;

import in.codefarm.product_service.dto.DeductInventoryRequest;
import in.codefarm.product_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inventory operations for HLD flow (deduct after payment success).
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final ProductService productService;

    @PostMapping("/deduct")
    public ResponseEntity<Void> deduct(@RequestBody DeductInventoryRequest request) {
        log.info("Deduct request orderId={}", request.getOrderId());
        productService.deductForOrder(request);
        return ResponseEntity.ok().build();
    }
}
