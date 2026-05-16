package in.codefarm.shipping_service.dto;

import in.codefarm.shipping_service.entity.Shipment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DeliveryResponse {

    private String deliveryId;
    private String orderId;
    private String customerId;
    private String shippingAddress;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DeliveryResponse fromShipment(Shipment s) {
        return DeliveryResponse.builder()
            .deliveryId(s.getShipmentId())
            .orderId(s.getOrderId())
            .customerId(s.getCustomerId())
            .shippingAddress(s.getShippingAddress())
            .status(s.getStatus().name())
            .createdAt(s.getCreatedAt())
            .updatedAt(s.getUpdatedAt())
            .build();
    }
}
