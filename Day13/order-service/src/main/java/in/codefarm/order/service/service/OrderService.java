package in.codefarm.order.service.service;

import in.codefarm.order.service.domain.Order;
import in.codefarm.order.service.domain.OutboxEvent;
import in.codefarm.order.service.model.OrderEvent;
import in.codefarm.order.service.model.OrderRequest;
import in.codefarm.order.service.repository.OrderRepository;
import in.codefarm.order.service.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;


    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository, OutboxEventRepository outboxEventRepository,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Order createOrder(OrderRequest request) {
        log.info("Received order for course {} for user {}", request.getCourseId(), request.getUserId());
        // 1. Save to database
        Order order = Order.builder()
                .userId(request.getUserId())
                .courseId(request.getCourseId())
                .amount(request.getAmount())
                .status("CREATED")
                .build();

        order = orderRepository.save(order); // infra 1 //write2
        log.info("Saved order for course {} for user {}", request.getCourseId(), request.getUserId());

        OrderEvent event = new OrderEvent(
                order.getId(),
                order.getUserId(),
                order.getCourseId(),
                order.getStatus(),
                "order-service"
        );

        // 2. Save Outbox Event (same transaction)
        OutboxEvent outbox = new OutboxEvent();
        outbox.setAggregateType("ORDER");
        outbox.setAggregateId(order.getId());
        outbox.setType("ORDER_CREATED");
        outbox.setPayload(objectMapper.writeValueAsString(event)); // assume ObjectMapper bean
        outbox.setStatus("NEW");
        outbox.setCreatedAt(Instant.now());

        outboxEventRepository.save(outbox);
        log.info("Saved order event for course {} for user {}", request.getCourseId(), request.getUserId());

        return order;
    }




















//    private void saveOutBoxEvent(OrderRequest request, Order order) {
//        // 2. Save Outbox Event (same transaction)
//
//        OutboxEvent outbox = new OutboxEvent();
//        outbox.setAggregateType("ORDER");
//        outbox.setAggregateId(order.getId());
//        outbox.setType("ORDER_CREATED");
//        outbox.setPayload(objectMapper.writeValueAsString(event)); // assume ObjectMapper bean
//        outbox.setStatus("NEW");
//        outbox.setCreatedAt(Instant.now());
//        outboxEventRepository.save(outbox);
//        log.info("Saved order event for course {} for user {}", request.getCourseId(), request.getUserId());
//    }
}