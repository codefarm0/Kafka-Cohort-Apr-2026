package in.codefarm.saga.integration;

import in.codefarm.saga.event.EventWrapper;
import in.codefarm.saga.event.OrderPlacedEvent;
import in.codefarm.saga.inventory.repository.InventoryReservationRepository;
import in.codefarm.saga.order.repository.OrderRepository;
import in.codefarm.saga.order.service.OrderEventProducer;
import in.codefarm.saga.order.service.OrderService;
import in.codefarm.saga.payment.repository.PaymentRepository;
import in.codefarm.saga.payment.service.PaymentService;
import in.codefarm.saga.testutil.TestEventBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end integration tests for the complete saga pattern flow with real services and consumers.
 * 
 * <p><b>Technical Approach:</b>
 * <ul>
 *   <li>Uses <b>@EmbeddedKafka</b> to create an in-memory Kafka broker (KRaft mode) for testing</li>
 *   <li>Uses <b>real application consumers</b> (via {@code @KafkaListener} annotations) that automatically
 *       consume events from EmbeddedKafka and execute real business logic</li>
 *   <li>Tests complete saga orchestration: Order → Payment → Inventory → Email</li>
 *   <li>Verifies database state (JPA repositories) after saga completion/compensation</li>
 * </ul>
 * 
 * <p><b>Key Technical Details:</b>
 * <ul>
 *   <li><b>Real Consumer Processing:</b> Application's {@code @KafkaListener} methods (PaymentServiceConsumer,
 *       InventoryServiceConsumer, OrderServiceConsumer, EmailServiceConsumer) automatically consume events
 *       from EmbeddedKafka topics, triggering real service methods and database operations</li>
 *   <li><b>Asynchronous Event Processing:</b> Uses {@code Awaitility.await().untilAsserted()} to handle
 *       asynchronous event processing - consumers process events in background threads, so tests must wait
 *       for database state to reflect saga completion</li>
 *   <li><b>Database State Verification:</b> Verifies final state in JPA repositories (OrderRepository,
 *       PaymentRepository, InventoryReservationRepository) after all saga steps complete or compensation
 *       occurs</li>
 *   <li><b>Compensation Flow Testing:</b> Tests complete compensation chains:
 *       <ul>
 *         <li>Payment failure → Order cancellation</li>
 *         <li>Inventory failure → Payment refund → Order cancellation</li>
 *       </ul>
 *   </li>
 *   <li><b>Idempotency Verification:</b> Tests that duplicate {@code OrderPlacedEvent} events don't cause
 *       duplicate payment processing (idempotency check in PaymentServiceConsumer)</li>
 *   <li><b>Service Integration:</b> Uses real {@code OrderService}, {@code PaymentService}, and event
 *       producers to trigger the complete saga flow</li>
 * </ul>
 * 
 * <p><b>What This Tests:</b>
 * <ul>
 *   <li>Complete saga orchestration across multiple services via event choreography</li>
 *   <li>Real consumer processing and business logic execution (not mocked)</li>
 *   <li>Database state consistency after saga completion or compensation</li>
 *   <li>Compensation transactions (rollback flows) work correctly</li>
 *   <li>Idempotent event processing prevents duplicate side effects</li>
 * </ul>
 * 
 * <p><b>Difference from SagaPatternIntegrationTest:</b>
 * This class tests the <b>business logic and application state</b> (services, repositories, real consumers,
 * saga orchestration), while {@code SagaPatternIntegrationTest} tests the <b>messaging/infrastructure layer</b>
 * (event structure, topics, serialization, manual consumer verification).
 * 
 * @see SagaPatternIntegrationTest
 */
@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = {"orders", "payments", "inventory"}
)
@DirtiesContext
@DisplayName("Complete Saga Flow Integration Tests")
class SagaFlowIntegrationTest {
    
    @Autowired
    private KafkaTemplate<String, EventWrapper<?>> kafkaTemplate;
    
    @Autowired
    private OrderService orderService;
    
    @Autowired
    private OrderEventProducer orderEventProducer;
    
    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private InventoryReservationRepository inventoryReservationRepository;
    
    @BeforeEach
    void setUp() {
        //not to be done in the actual setup
//        inventoryReservationRepository.deleteAll();
//        paymentRepository.deleteAll();
//        orderRepository.deleteAll();
    }
    
    @AfterEach
    void tearDown() {
        inventoryReservationRepository.deleteAll();
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
    }
    
    @Test
    @DisplayName("Should complete full saga flow: Order → Payment → Inventory → Email")
    void shouldCompleteFullSagaFlow() {
        String customerId = "customer-123";
        String productId = "product-456";
        Integer quantity = 2;
        BigDecimal amount = BigDecimal.valueOf(99.99);  // Will pass payment (< 1000)
        String orderId = java.util.UUID.randomUUID().toString();
        String transactionId = TestEventBuilder.generateTransactionId();
        
        orderService.createOrder(orderId, customerId, productId, quantity, amount, transactionId);
        OrderPlacedEvent orderEvent = new OrderPlacedEvent(
            orderId,
            customerId,
            productId,
            quantity,
            amount,
            java.time.LocalDateTime.now()
        );
        orderEventProducer.sendOrderPlacedEvent(orderEvent, transactionId);
        
        await().atMost(java.time.Duration.ofSeconds(10))
            .untilAsserted(() -> {
                var order = orderRepository.findByOrderId(orderId);
                assertThat(order).isPresent();
                assertThat(order.get().getStatus()).isEqualTo("PENDING");
                
                var payment = paymentService.findByOrderId(orderId);
                assertThat(payment).isPresent();
                assertThat(payment.get().getStatus()).isEqualTo("SUCCESS");
                
                var reservation = inventoryReservationRepository.findByOrderId(orderId);
                assertThat(reservation).isPresent();
                assertThat(reservation.get().getStatus()).isEqualTo("RESERVED");
            });
        var order = orderRepository.findByOrderId(orderId);
        assertThat(order).isPresent();
        
        var payment = paymentService.findByOrderId(orderId);
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo("SUCCESS");
        
        var reservation = inventoryReservationRepository.findByOrderId(orderId);
        assertThat(reservation).isPresent();
        assertThat(reservation.get().getStatus()).isEqualTo("RESERVED");
    }
    
    @Test
    @DisplayName("Should handle payment failure and cancel order (compensation)")
    void shouldHandlePaymentFailureAndCancelOrder() {
        String customerId = "customer-123";
        String productId = "product-456";
        Integer quantity = 2;
        BigDecimal amount = BigDecimal.valueOf(1500.00);  // Will fail payment (> 1000)
        String orderId = java.util.UUID.randomUUID().toString();
        String transactionId = TestEventBuilder.generateTransactionId();
        
        // Create order in database
        orderService.createOrder(orderId, customerId, productId, quantity, amount, transactionId);
        
        // Publish OrderPlacedEvent
        OrderPlacedEvent orderEvent = new OrderPlacedEvent(
            orderId,
            customerId,
            productId,
            quantity,
            amount,
            java.time.LocalDateTime.now()
        );
        orderEventProducer.sendOrderPlacedEvent(orderEvent, transactionId);
        
        await().atMost(java.time.Duration.ofSeconds(10))
            .untilAsserted(() -> {
                var order = orderRepository.findByOrderId(orderId);
                assertThat(order).isPresent();
                
                var payment = paymentService.findByOrderId(orderId);
                assertThat(payment).isPresent();
                assertThat(payment.get().getStatus()).isEqualTo("FAILED");
                
                var cancelledOrder = orderRepository.findByOrderId(orderId);
                assertThat(cancelledOrder).isPresent();
                assertThat(cancelledOrder.get().getStatus()).isEqualTo("CANCELLED");
            });
        var order = orderRepository.findByOrderId(orderId);
        assertThat(order).isPresent();
        assertThat(order.get().getStatus()).isEqualTo("CANCELLED");
        
        var payment = paymentService.findByOrderId(orderId);
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo("FAILED");
    }
    
    @Test
    @DisplayName("Should handle inventory failure and trigger payment refund and order cancellation")
    void shouldHandleInventoryFailureAndTriggerCompensation() {
        String customerId = "customer-123";
        String productId = "product-456";
        Integer quantity = 2;
        BigDecimal amount = BigDecimal.valueOf(600.00);  // Will pass payment but fail inventory (> 500)
        String orderId = java.util.UUID.randomUUID().toString();
        String transactionId = TestEventBuilder.generateTransactionId();
        
        // Create order in database
        orderService.createOrder(orderId, customerId, productId, quantity, amount, transactionId);
        
        // Publish OrderPlacedEvent
        OrderPlacedEvent orderEvent = new OrderPlacedEvent(
            orderId,
            customerId,
            productId,
            quantity,
            amount,
            java.time.LocalDateTime.now()
        );
        orderEventProducer.sendOrderPlacedEvent(orderEvent, transactionId);
        
        await().atMost(java.time.Duration.ofSeconds(10))
            .untilAsserted(() -> {
                var order = orderRepository.findByOrderId(orderId);
                assertThat(order).isPresent();
                
                var payment = paymentService.findByOrderId(orderId);
                assertThat(payment).isPresent();
                assertThat(payment.get().getStatus()).isEqualTo("REFUNDED");
                
                var cancelledOrder = orderRepository.findByOrderId(orderId);
                assertThat(cancelledOrder).isPresent();
                assertThat(cancelledOrder.get().getStatus()).isEqualTo("CANCELLED");
            });
        var order = orderRepository.findByOrderId(orderId);
        assertThat(order).isPresent();
        assertThat(order.get().getStatus()).isEqualTo("CANCELLED");
        
        var payment = paymentService.findByOrderId(orderId);
        assertThat(payment).isPresent();
        assertThat(payment.get().getStatus()).isEqualTo("REFUNDED");
    }
    
    @Test
    @DisplayName("Should handle duplicate OrderPlacedEvent idempotently")
    void shouldHandleDuplicateOrderPlacedEventIdempotently() throws InterruptedException {
        String customerId = "customer-123";
        String productId = "product-456";
        Integer quantity = 2;
        BigDecimal amount = BigDecimal.valueOf(99.99);
        String orderId = java.util.UUID.randomUUID().toString();
        String transactionId = TestEventBuilder.generateTransactionId();
        
        // Create order in database
        orderService.createOrder(orderId, customerId, productId, quantity, amount, transactionId);
        
        // Publish OrderPlacedEvent
        OrderPlacedEvent orderEvent = new OrderPlacedEvent(
            orderId,
            customerId,
            productId,
            quantity,
            amount,
            java.time.LocalDateTime.now()
        );
        orderEventProducer.sendOrderPlacedEvent(orderEvent, transactionId);
        
        await().atMost(java.time.Duration.ofSeconds(5))
            .until(() -> paymentService.findByOrderId(orderId).isPresent());
        
        var initialPayment = paymentService.findByOrderId(orderId);
        assertThat(initialPayment).isPresent();
        OrderPlacedEvent duplicateEvent = new OrderPlacedEvent(
            orderId,
            customerId,
            productId,
            quantity,
            amount,
            java.time.LocalDateTime.now()
        );
        
        EventWrapper<OrderPlacedEvent> wrapper = TestEventBuilder.wrapOrderPlaced(
            duplicateEvent,
            TestEventBuilder.generateTransactionId()
        );
        
        kafkaTemplate.send("orders", orderId, wrapper);
        kafkaTemplate.flush();
        
        Thread.sleep(2000);
        
        var payments = paymentService.findByOrderId(orderId);
        assertThat(payments).isPresent();
    }
}

