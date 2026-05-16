package in.codefarm.order_service.service;

import in.codefarm.order_service.dto.CreateOrderRequest;
import in.codefarm.order_service.dto.OrderResponse;
import in.codefarm.order_service.entity.Order;
import in.codefarm.order_service.entity.OrderItem;
import in.codefarm.order_service.event.OrderPlacedEventData;
import in.codefarm.order_service.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Order Service: persists orders, publishes order-created to Kafka, Redis reservation (1 min TTL).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    public static final String ORDERS_TOPIC = "orders";
    public static final String ORDER_PLACED_EVENT_TYPE = "com.ecommerce.order.placed";

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RedisReservationService redisReservationService;
    private final InventoryServiceClient inventoryServiceClient;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());

        String orderId = "order-" + UUID.randomUUID().toString();

        BigDecimal totalAmount = request.getItems().stream()
            .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
            .id(orderId)
            .customerId(request.getCustomerId())
            .totalAmount(totalAmount)
            .status(Order.OrderStatus.PENDING)
            .shippingAddress(request.getShippingAddress())
            .build();

        final Order orderRef = order;
        var orderItems = request.getItems().stream()
            .map(itemRequest -> {
                BigDecimal itemTotal = itemRequest.getUnitPrice()
                    .multiply(BigDecimal.valueOf(itemRequest.getQuantity()));
                return OrderItem.builder()
                    .order(orderRef)
                    .productId(itemRequest.getProductId())
                    .productName(itemRequest.getProductName())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(itemRequest.getUnitPrice())
                    .totalPrice(itemTotal)
                    .build();
            })
            .collect(Collectors.toList());

        order.setItems(orderItems);
        order = orderRepository.save(order);
        log.info("Order saved: {}", orderId);

        List<OrderPlacedEventData.OrderItemData> itemDataList = orderItems.stream()
            .map(item -> OrderPlacedEventData.OrderItemData.builder()
                .productId(item.getProductId())
                .productName(item.getProductName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .build())
            .collect(Collectors.toList());

        OrderPlacedEventData eventData = OrderPlacedEventData.builder()
            .orderId(orderId)
            .customerId(request.getCustomerId())
            .totalAmount(totalAmount)
            .items(itemDataList)
            .shippingAddress(request.getShippingAddress())
            .build();

        String eventId = UUID.randomUUID().toString();
        String source = "/order-service";
        Map<String, Object> eventWrapper = new HashMap<>();
        eventWrapper.put("source", source);
        eventWrapper.put("eventType", ORDER_PLACED_EVENT_TYPE);
        eventWrapper.put("eventId", eventId);
        eventWrapper.put("eventTime", java.time.OffsetDateTime.now().toString());
        eventWrapper.put("data", eventData);

        final String eventJson;
        try {
            eventJson = objectMapper.writeValueAsString(eventWrapper);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize order event", e);
        }

        final List<OrderPlacedEventData.OrderItemData> itemsForRedis = new ArrayList<>(itemDataList);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    kafkaTemplate.send(ORDERS_TOPIC, orderId, eventJson).get(30, TimeUnit.SECONDS);
                    log.info("Published order-created to Kafka: orderId={}", orderId);
                    redisReservationService.reserveOrder(orderId, itemsForRedis);
                } catch (Exception e) {
                    log.error("afterCommit failed for orderId={}: Kafka or Redis error. Order exists in DB without downstream trigger.",
                        orderId, e);
                }
            }
        });

        return OrderResponse.fromEntity(order);
    }

    public OrderResponse getOrder(String orderId) {
        log.info("Retrieving order: {}", orderId);
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        return OrderResponse.fromEntity(order);
    }

    @Transactional
    public void updateOrderStatus(String orderId, Order.OrderStatus status) {
        log.info("Updating order status: orderId={}, status={}", orderId, status);
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        order.setStatus(status);
        orderRepository.save(order);
    }

    /**
     * After successful payment: release Redis reservation, deduct inventory, set PAYMENT_SUCCEEDED.
     */
    @Transactional
    public void onPaymentProcessed(String orderId, String paymentEventId,
                                   List<InventoryServiceClient.DeductItem> deductItems) {
        List<InventoryServiceClient.DeductItem> items = deductItems;
        if (items == null || items.isEmpty()) {
            Order o = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
            items = o.getItems().stream()
                .map(i -> new InventoryServiceClient.DeductItem(i.getProductId(), i.getQuantity()))
                .collect(Collectors.toList());
        }
        inventoryServiceClient.deductInventory(orderId, items);
        redisReservationService.releaseReservation(orderId);
        updateOrderStatus(orderId, Order.OrderStatus.PAYMENT_SUCCEEDED);
    }

    @Transactional
    public void onPaymentFailed(String orderId) {
        redisReservationService.releaseReservation(orderId);
        updateOrderStatus(orderId, Order.OrderStatus.PAYMENT_FAILED);
    }

    @Transactional
    public void onDeliveryShipped(String orderId) {
        updateOrderStatus(orderId, Order.OrderStatus.SHIPPED);
    }

    @Transactional
    public void onDeliveryDelivered(String orderId) {
        updateOrderStatus(orderId, Order.OrderStatus.DELIVERED);
    }

    public List<OrderResponse> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
            .map(OrderResponse::fromEntity)
            .collect(Collectors.toList());
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
            .map(OrderResponse::fromEntity)
            .collect(Collectors.toList());
    }

    public List<OrderResponse> createBatchOrders(CreateOrderRequest templateRequest, int count) {
        log.info("Creating batch orders: count={}, customerId={}", count, templateRequest.getCustomerId());
        List<OrderResponse> orders = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            try {
                CreateOrderRequest request = new CreateOrderRequest(
                    templateRequest.getCustomerId() + "-batch-" + i,
                    templateRequest.getItems(),
                    templateRequest.getShippingAddress()
                );
                orders.add(createOrder(request));
                if (i % 10 == 0 && i > 0) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("Failed to create order in batch: index={}", i, e);
            }
        }
        log.info("Batch orders created: successful={}, total={}", orders.size(), count);
        return orders;
    }
}
