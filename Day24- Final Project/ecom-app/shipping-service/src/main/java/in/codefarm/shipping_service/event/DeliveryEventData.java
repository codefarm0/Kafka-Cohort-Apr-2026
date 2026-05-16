package in.codefarm.shipping_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryEventData {

    private String deliveryId;
    private String orderId;
    private String status;
    private Instant timestamp;
}
