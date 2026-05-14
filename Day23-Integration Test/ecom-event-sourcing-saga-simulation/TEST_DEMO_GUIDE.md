# Test Demo Guide - Saga Pattern Integration Tests

This guide explains how to run and demonstrate the comprehensive test suite for the Saga Pattern project.

## Overview

The test suite demonstrates:
1. **Unit Tests**: Fast tests with mocks (business logic only)
2. **Integration Tests**: Complete saga flow tests with Embedded Kafka
3. **Real Services**: Tests use actual services and consumers
4. **Multiple Scenarios**: Success, failure, and compensation flows

## Test Structure

```
src/test/java/in/codefarm/saga/
├── testutil/
│   └── TestEventBuilder.java          # Helper for creating test events
├── unit/
│   ├── OrderEventProducerTest.java     # Unit tests (mocks)
│   └── PaymentEventProducerTest.java    # Unit tests (mocks)
└── integration/
    ├── SagaPatternIntegrationTest.java  # Event flow verification
    └── SagaFlowIntegrationTest.java    # Complete saga flow with services
```

## Running Tests

### Run All Tests

```bash
cd exploration/ecom-saga-pattern-simulation
./gradlew test
```

### Run Specific Test Class

```bash
# Unit tests
./gradlew test --tests "OrderEventProducerTest"

# Integration tests
./gradlew test --tests "SagaFlowIntegrationTest"
```

### Run with Detailed Output

```bash
./gradlew test --info
```

### Run Single Test Method

```bash
./gradlew test --tests "SagaFlowIntegrationTest.shouldCompleteFullSagaFlow"
```

## Demo Script

### Demo 1: Unit Tests (Fast, Mock-based)

**Purpose**: Show fast unit testing with mocks.

**Command**:
```bash
./gradlew test --tests "OrderEventProducerTest" --info
```

**What to Explain**:
- "These are unit tests using mocks"
- "No real Kafka - tests run in milliseconds"
- "Tests verify business logic: event creation, metadata, error handling"
- "Use these for fast feedback during development"

**Key Points**:
- ✅ Fast execution
- ✅ No external dependencies
- ✅ Tests business logic
- ❌ Doesn't test real Kafka integration

### Demo 2: Integration Test - Successful Saga Flow

**Purpose**: Show complete saga flow working end-to-end.

**Command**:
```bash
./gradlew test --tests "SagaFlowIntegrationTest.shouldCompleteFullSagaFlow" --info
```

**What to Explain**:
- "This test uses Embedded Kafka - a real Kafka broker in memory"
- "It tests the complete saga flow: Order → Payment → Inventory → Email"
- "All services are real - they consume and produce events"
- "We verify the final state in the database"

**Flow Demonstrated**:
1. Order is placed → `OrderPlacedEvent` published
2. Payment service consumes → processes payment → `PaymentProcessedEvent` (SUCCESS) published
3. Inventory service consumes → reserves inventory → `InventoryReservedEvent` (RESERVED) published
4. Email service consumes → sends confirmation

**Verification**:
- ✅ Order created in database
- ✅ Payment processed and saved
- ✅ Inventory reserved and saved

**Demo Talking Points**:
- "Notice how each service automatically processes events"
- "The transactionId links all events together"
- "This is the saga pattern in action"

### Demo 3: Integration Test - Payment Failure Compensation

**Purpose**: Show compensation when payment fails.

**Command**:
```bash
./gradlew test --tests "SagaFlowIntegrationTest.shouldHandlePaymentFailureAndCancelOrder" --info
```

**What to Explain**:
- "This test shows what happens when payment fails"
- "Order amount > 1000 triggers payment failure"
- "Order service compensates by cancelling the order"
- "This is the saga compensation pattern"

**Flow Demonstrated**:
1. Order is placed
2. Payment fails (amount > 1000)
3. Order service compensates → `OrderCancelledEvent` published

**Verification**:
- ✅ Order created
- ✅ Payment failed
- ✅ Order cancelled (compensation)

**Demo Talking Points**:
- "When payment fails, we need to rollback (cancel the order)"
- "Notice how OrderCancelledEvent is published automatically"
- "This ensures data consistency across services"

### Demo 4: Integration Test - Inventory Failure Compensation

**Purpose**: Show compensation chain when inventory fails.

**Command**:
```bash
./gradlew test --tests "SagaFlowIntegrationTest.shouldHandleInventoryFailureAndTriggerCompensation" --info
```

**What to Explain**:
- "This test shows compensation when inventory reservation fails"
- "Payment already succeeded, so we need to refund it"
- "Then cancel the order"
- "This demonstrates the compensation chain"

**Flow Demonstrated**:
1. Order is placed
2. Payment succeeds
3. Inventory reservation fails (amount > 500)
4. Payment refunded → `PaymentRefundedEvent` published
5. Order cancelled → `OrderCancelledEvent` published

**Verification**:
- ✅ Order created
- ✅ Payment succeeded initially
- ✅ Inventory reservation failed
- ✅ Payment refunded (compensation)
- ✅ Order cancelled (compensation)

**Demo Talking Points**:
- "When inventory fails after payment succeeds, we need to compensate both"
- "Notice the compensation chain: Inventory fails → Payment refunded → Order cancelled"
- "This shows how saga handles partial completion"

### Demo 5: Integration Test - Idempotency

**Purpose**: Show idempotency handling for duplicate events.

**Command**:
```bash
./gradlew test --tests "SagaFlowIntegrationTest.shouldHandleDuplicateOrderPlacedEventIdempotently" --info
```

**What to Explain**:
- "This test verifies idempotency - processing same event twice"
- "Even if OrderPlacedEvent is processed twice, only one payment is created"
- "This is critical for exactly-once semantics"

**Flow Demonstrated**:
1. Order is placed → Payment processed
2. Same OrderPlacedEvent processed again (duplicate)
3. Payment service skips processing (idempotency check)

**Verification**:
- ✅ Only one payment created
- ✅ Duplicate event is ignored

**Demo Talking Points**:
- "Idempotency ensures duplicate events don't cause duplicate processing"
- "This is critical for exactly-once semantics"
- "Notice how the payment service checks if payment already exists"

## Test Explanations

### TestEventBuilder

**Purpose**: Creates consistent test events with proper metadata.

**Why it's useful**:
- Reduces test code duplication
- Ensures all events have proper metadata
- Makes tests more maintainable

**Example Usage**:
```java
// Create default order event
OrderPlacedEvent event = TestEventBuilder.defaultOrderPlacedEvent();

// Create order with specific amount (for failure scenarios)
OrderPlacedEvent event = TestEventBuilder.orderPlacedEventWithAmount(
    BigDecimal.valueOf(1500.00)  // Will fail payment
);

// Wrap with metadata
EventWrapper<OrderPlacedEvent> wrapper = TestEventBuilder.wrapOrderPlaced(
    event,
    transactionId
);
```

### Embedded Kafka

**What it is**: In-memory Kafka broker that runs in your test JVM.

**Benefits**:
- ✅ Real Kafka broker (not a mock)
- ✅ No external setup required
- ✅ Fast startup (~1-2 seconds)
- ✅ Isolated tests
- ✅ Automatic cleanup

**Configuration**:
```java
@EmbeddedKafka(
    partitions = 1,
    topics = {"orders", "payments", "inventory"},
    brokerProperties = {
        "listeners=PLAINTEXT://localhost:9092",
        "port=9092"
    }
)
```

### Awaitility

**What it is**: Library for waiting for asynchronous conditions.

**Why it's needed**: Kafka processing is asynchronous - we need to wait for services to process events.

**Example**:
```java
await().atMost(Duration.ofSeconds(10))
    .untilAsserted(() -> {
        var payment = paymentService.findByOrderId(orderId);
        assertThat(payment).isPresent();
    });
```

## Common Issues and Solutions

### Issue 1: Tests Timing Out

**Symptom**: Tests fail with timeout errors.

**Cause**: Services not processing events fast enough.

**Solution**:
- Increase timeout in `await().atMost()` calls
- Check that all services are properly configured
- Verify Embedded Kafka is starting correctly

### Issue 2: Port Already in Use

**Symptom**: `Failed to start embedded Kafka` or port conflict.

**Cause**: Another Kafka instance running on port 9092.

**Solution**:
```bash
# Stop other Kafka instances
docker-compose down

# Or change port in @EmbeddedKafka annotation
brokerProperties = {"listeners=PLAINTEXT://localhost:9093", "port=9093"}
```

### Issue 3: Deserialization Errors

**Symptom**: `ClassNotFoundException` or deserialization errors.

**Cause**: Consumer deserializer not matching producer serializer.

**Solution**:
- Ensure test consumers use same deserializer as production
- Check `TRUSTED_PACKAGES` configuration
- Verify `JsonMapper` is configured correctly

### Issue 4: Database State Issues

**Symptom**: Tests interfering with each other.

**Cause**: Data from previous tests not cleaned up.

**Solution**:
- Tests use `@BeforeEach` and `@AfterEach` to clean up
- Ensure H2 in-memory database is used
- Check that repositories are properly injected

## Best Practices Demonstrated

### 1. Test Isolation

- Each test is independent
- Database is cleaned before/after each test
- Unique consumer groups per test

### 2. Realistic Testing

- Uses Embedded Kafka (real broker, not mocks)
- Tests complete flows, not just individual components
- Verifies database state, not just events

### 3. Clear Test Names

- Test names describe what they test
- `@DisplayName` provides readable descriptions
- Comments explain demo scenarios

### 4. Test Data Builders

- `TestEventBuilder` creates consistent test data
- Reduces test code duplication
- Makes tests more maintainable

## Demo Flow Recommendations

### For Quick Demo (5 minutes)

1. Run unit test: `OrderEventProducerTest`
   - Explain: "Fast unit tests with mocks"

2. Run successful flow: `SagaFlowIntegrationTest.shouldCompleteFullSagaFlow`
   - Explain: "Complete saga flow with Embedded Kafka"

### For Detailed Demo (15 minutes)

1. **Unit Tests** (2 min)
   - Show `OrderEventProducerTest`
   - Explain mocks vs real Kafka

2. **Successful Flow** (5 min)
   - Run `SagaFlowIntegrationTest.shouldCompleteFullSagaFlow`
   - Show event flow
   - Check database state

3. **Payment Failure** (4 min)
   - Run `SagaFlowIntegrationTest.shouldHandlePaymentFailureAndCancelOrder`
   - Explain compensation

4. **Inventory Failure** (4 min)
   - Run `SagaFlowIntegrationTest.shouldHandleInventoryFailureAndTriggerCompensation`
   - Explain compensation chain

## Key Takeaways for Audience

1. **Unit Tests**: Fast, isolated, test business logic with mocks
2. **Integration Tests**: Real Kafka, test complete flows
3. **Embedded Kafka**: Real broker, no external setup
4. **Test Isolation**: Each test is independent
5. **Realistic Testing**: Tests use actual services
6. **Multiple Scenarios**: Success, failure, compensation

## Next Steps

- Add more edge case tests
- Add performance tests
- Add chaos engineering tests (simulate failures)
- Add contract tests (verify event schemas)

