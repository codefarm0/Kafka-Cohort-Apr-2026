package in.codefarm.shipping_service.service;

import in.codefarm.shipping_service.dto.DeliveryResponse;
import in.codefarm.shipping_service.entity.Shipment;
import in.codefarm.shipping_service.event.DeliveryEventData;
import in.codefarm.shipping_service.event.EventService;
import in.codefarm.shipping_service.exception.DeliveryEventPublishingException;
import in.codefarm.shipping_service.repository.ShipmentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryService {

    public static final String DELIVERIES_TOPIC = "deliveries";

    private final ShipmentRepository shipmentRepository;
    private final RedisIdempotencyService redisIdempotency;
    private final EventService eventService;
    @Qualifier("transactionalKafkaTemplate")
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public void createAwaitingShipmentAfterPayment(String orderId, String customerId, String shippingAddress,
                                                   String paymentEventId) {
        String idem = "idempotency:delivery:payment:" + paymentEventId;
        if (!redisIdempotency.claimOnce24h(idem)) {
            log.debug("Skip duplicate payment event for delivery: {}", paymentEventId);
            return;
        }
        boolean exists = shipmentRepository.findByOrderId(orderId).stream()
            .anyMatch(s -> s.getStatus() == Shipment.ShipmentStatus.AWAITING_SHIPMENT
                || s.getStatus() == Shipment.ShipmentStatus.SHIPPED
                || s.getStatus() == Shipment.ShipmentStatus.DELIVERED);
        if (exists) {
            log.info("Delivery already exists for orderId={}", orderId);
            return;
        }
        String deliveryId = "delivery-" + UUID.randomUUID();
        Shipment shipment = Shipment.builder()
            .shipmentId(deliveryId)
            .orderId(orderId)
            .customerId(customerId != null ? customerId : "unknown")
            .shippingAddress(shippingAddress != null ? shippingAddress : "")
            .status(Shipment.ShipmentStatus.AWAITING_SHIPMENT)
            .build();
        shipmentRepository.save(shipment);
        log.info("Delivery AWAITING_SHIPMENT: orderId={}, deliveryId={}", orderId, deliveryId);
    }

    public List<DeliveryResponse> listByStatus(String statusParam) {
        if (statusParam == null || statusParam.isBlank()) {
            return shipmentRepository.findAll().stream()
                .map(DeliveryResponse::fromShipment)
                .collect(Collectors.toList());
        }
        Shipment.ShipmentStatus status = Shipment.ShipmentStatus.valueOf(statusParam.trim().toUpperCase());
        return shipmentRepository.findByStatus(status).stream()
            .map(DeliveryResponse::fromShipment)
            .collect(Collectors.toList());
    }

    @Transactional
    public DeliveryResponse updateStatus(String orderId, String newStatus) {
        String upper = newStatus.trim().toUpperCase();
        if (!"SHIPPED".equals(upper) && !"DELIVERED".equals(upper)) {
            throw new IllegalArgumentException("status must be SHIPPED or DELIVERED");
        }

        Shipment shipment = latestOpenShipment(orderId);

        if ("SHIPPED".equals(upper)) {
            if (shipment.getStatus() == Shipment.ShipmentStatus.SHIPPED
                || shipment.getStatus() == Shipment.ShipmentStatus.DELIVERED) {
                return DeliveryResponse.fromShipment(shipment);
            }
            if (shipment.getStatus() != Shipment.ShipmentStatus.AWAITING_SHIPMENT) {
                throw new IllegalStateException("Expected AWAITING_SHIPMENT, was " + shipment.getStatus());
            }
            if (!redisIdempotency.claimOnce("idempotency:delivery:rest:" + orderId + ":SHIPPED", Duration.ofDays(7))) {
                Shipment reloaded = latestOpenShipment(orderId);
                if (reloaded.getStatus() == Shipment.ShipmentStatus.SHIPPED
                    || reloaded.getStatus() == Shipment.ShipmentStatus.DELIVERED) {
                    return DeliveryResponse.fromShipment(reloaded);
                }
                throw new IllegalStateException("Concurrent SHIPPED update; retry");
            }
            shipment.setStatus(Shipment.ShipmentStatus.SHIPPED);
            shipmentRepository.save(shipment);
            publishToDeliveriesTopic(shipment, "SHIPPED");
            return DeliveryResponse.fromShipment(shipment);
        }

        if (shipment.getStatus() == Shipment.ShipmentStatus.DELIVERED) {
            return DeliveryResponse.fromShipment(shipment);
        }
        if (shipment.getStatus() != Shipment.ShipmentStatus.SHIPPED) {
            throw new IllegalStateException("Expected SHIPPED before DELIVERED, was " + shipment.getStatus());
        }
        if (!redisIdempotency.claimOnce("idempotency:delivery:rest:" + orderId + ":DELIVERED", Duration.ofDays(7))) {
            Shipment reloaded = latestOpenShipment(orderId);
            if (reloaded.getStatus() == Shipment.ShipmentStatus.DELIVERED) {
                return DeliveryResponse.fromShipment(reloaded);
            }
            throw new IllegalStateException("Concurrent DELIVERED update; retry");
        }
        shipment.setStatus(Shipment.ShipmentStatus.DELIVERED);
        shipmentRepository.save(shipment);
        publishToDeliveriesTopic(shipment, "DELIVERED");
        return DeliveryResponse.fromShipment(shipment);
    }

    private Shipment latestOpenShipment(String orderId) {
        return shipmentRepository.findByOrderId(orderId).stream()
            .filter(s -> s.getStatus() != Shipment.ShipmentStatus.FAILED)
            .max(Comparator.comparing(Shipment::getId))
            .orElseThrow(() -> new IllegalStateException("No delivery for order: " + orderId));
    }

    private void publishToDeliveriesTopic(Shipment shipment, String status) {
        try {
            DeliveryEventData data = DeliveryEventData.builder()
                .deliveryId(shipment.getShipmentId())
                .orderId(shipment.getOrderId())
                .status(status)
                .timestamp(Instant.now())
                .build();
            String eventType = "DELIVERED".equals(status)
                ? "com.ecommerce.delivery.delivered"
                : "com.ecommerce.delivery.shipped";
            String json = eventService.createEvent(eventType, data);
            kafkaTemplate.send(DELIVERIES_TOPIC, shipment.getOrderId(), json);
            log.info("Published to {}: orderId={}, status={}", DELIVERIES_TOPIC, shipment.getOrderId(), status);
        } catch (Exception e) {
            log.error("Failed to publish delivery event orderId={}", shipment.getOrderId(), e);
            throw new DeliveryEventPublishingException("Failed to publish delivery event", e);
        }
    }
}
