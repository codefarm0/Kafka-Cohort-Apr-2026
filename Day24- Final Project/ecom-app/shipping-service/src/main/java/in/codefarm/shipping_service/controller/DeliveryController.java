package in.codefarm.shipping_service.controller;

import in.codefarm.shipping_service.dto.DeliveryResponse;
import in.codefarm.shipping_service.service.DeliveryService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Simulated delivery-person API (HLD): list awaiting / mark SHIPPED or DELIVERED.
 */
@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
@Slf4j
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping
    public ResponseEntity<List<DeliveryResponse>> list(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(deliveryService.listByStatus(status));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<DeliveryResponse> updateStatus(
            @PathVariable String orderId,
            @RequestBody UpdateStatusRequest body) {
        log.info("PATCH delivery status orderId={}, status={}", orderId, body.getStatus());
        DeliveryResponse r = deliveryService.updateStatus(orderId, body.getStatus());
        return ResponseEntity.ok(r);
    }

    @Data
    public static class UpdateStatusRequest {
        @NotBlank
        private String status;
    }
}
