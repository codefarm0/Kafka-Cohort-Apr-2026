# Testing Guide for Saga Pattern Project

This document explains the comprehensive test suite for the Saga Pattern project, designed for demonstrations and learning.

## Test Structure

```
src/test/java/in/codefarm/saga/
├── testutil/
│   └── TestEventBuilder.java          # Test data builders
├── unit/
│   ├── OrderEventProducerTest.java     # Unit tests for order producer
│   └── PaymentEventProducerTest.java   # Unit tests for payment producer
└── integration/
    ├── SagaPatternIntegrationTest.java # Integration tests with Embedded Kafka
    └── SagaFlowIntegrationTest.java    # Complete saga flow tests
```

## Test Types

### 1. Unit Tests (`unit/`)

**Purpose**: Test individual components in isolation using mocks.

**Characteristics**:
- ⚡ Fast execution (no Kafka)
- ✅ Tests business logic
- ✅ Uses mocks for KafkaTemplate
- ✅ No external dependencies

**Example**: `OrderEventProducerTest`
- Tests that producer creates correct EventWrapper
- Tests metadata is set correctly
- Tests error handling

**When to use**: During development, for fast feedback on business logic.

### 2. Integration Tests (`integration/`)

**Purpose**: Test complete saga flows with real Kafka (Embedded Kafka).

**Characteristics**:
- 🔄 Uses Embedded Kafka (in-memory broker)
- ✅ Tests real event flow
- ✅ Tests service interactions
- ✅ Tests database persistence

**Example**: `SagaFlowIntegrationTest`
- Tests complete order → payment → inventory flow
- Tests compensation flows
- Tests idempotency

**When to use**: Before deployment, to verify complete integration.

## Running Tests

### Run All Tests

```bash
cd exploration/ecom-saga-pattern-simulation
./gradlew test
```

### Run Specific Test Class

```bash
# Unit tests
./gradlew test --tests "in.codefarm.saga.unit.OrderEventProducerTest"

# Integration tests
./gradlew test --tests "in.codefarm.saga.integration.SagaFlowIntegrationTest"
```

### Run with Coverage

```bash
./gradlew test jacocoTestReport
```

## Test Scenarios

### Scenario 1: Successful Saga Flow

**Test**: `SagaFlowIntegrationTest.shouldCompleteFullSagaFlow()`

**Flow**:
1. Order is placed → `OrderPlacedEvent` published
2. Payment service processes → `PaymentProcessedEvent` (SUCCESS) published
3. Inventory service reserves → `InventoryReservedEvent` (RESERVED) published
4. Email service sends confirmation

**Verification**:
- ✅ Order created in database
- ✅ Payment processed and saved
- ✅ Inventory reserved and saved

**Demo Talking Points**:
- "This shows the complete happy path"
- "Each service consumes events and publishes new events"
- "Transaction ID links all events together"

### Scenario 2: Payment Failure Compensation

**Test**: `SagaFlowIntegrationTest.shouldHandlePaymentFailureAndCancelOrder()`

**Flow**:
1. Order is placed
2. Payment fails (amount > 1000)
3. Order service compensates → `OrderCancelledEvent` published

**Verification**:
- ✅ Order created
- ✅ Payment failed
- ✅ Order cancelled (compensation)

**Demo Talking Points**:
- "When payment fails, we compensate by cancelling the order"
- "This is the saga compensation pattern"
- "Notice how OrderCancelledEvent is published to rollback"

### Scenario 3: Inventory Failure Compensation

**Test**: `SagaFlowIntegrationTest.shouldHandleInventoryFailureAndTriggerCompensation()`

**Flow**:
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
- "When inventory fails, we compensate both payment and order"
- "Notice the compensation chain: Inventory fails → Payment refunded → Order cancelled"
- "This shows how saga handles partial completion"

### Scenario 4: Idempotency

**Test**: `SagaFlowIntegrationTest.shouldHandleDuplicateOrderPlacedEventIdempotently()`

**Flow**:
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

## Test Utilities

### TestEventBuilder

**Purpose**: Create consistent test events with proper metadata.

**Usage**:
```java
// Create default order event
OrderPlacedEvent event = TestEventBuilder.defaultOrderPlacedEvent();

// Create order with specific amount
OrderPlacedEvent event = TestEventBuilder.orderPlacedEventWithAmount(
    BigDecimal.valueOf(1500.00)
);

// Create payment success event
PaymentProcessedEvent event = TestEventBuilder.paymentProcessedSuccess(
    "order-123",
    "customer-456",
    BigDecimal.valueOf(99.99)
);

// Wrap event with metadata
EventWrapper<OrderPlacedEvent> wrapper = TestEventBuilder.wrapOrderPlaced(
    event,
    transactionId
);
```

## Demo Script

### Step 1: Run Successful Flow Test

```bash
./gradlew test --tests "SagaFlowIntegrationTest.shouldCompleteFullSagaFlow" --info
```

**Explain**:
- "This test shows the complete saga flow"
- "Notice how each service processes events automatically"
- "All data is persisted in the database"

### Step 2: Run Payment Failure Test

```bash
./gradlew test --tests "SagaFlowIntegrationTest.shouldHandlePaymentFailureAndCancelOrder" --info
```

**Explain**:
- "This test shows compensation when payment fails"
- "Notice how the order is automatically cancelled"
- "This is the saga compensation pattern"

### Step 3: Run Inventory Failure Test

```bash
./gradlew test --tests "SagaFlowIntegrationTest.shouldHandleInventoryFailureAndTriggerCompensation" --info
```

**Explain**:
- "This test shows compensation when inventory fails"
- "Notice the compensation chain: Inventory fails → Payment refunded → Order cancelled"
- "This demonstrates how saga handles partial completion"

### Step 4: Run Idempotency Test

```bash
./gradlew test --tests "SagaFlowIntegrationTest.shouldHandleDuplicateOrderPlacedEventIdempotently" --info
```

**Explain**:
- "This test shows idempotency handling"
- "Even if the same event is processed twice, only one payment is created"
- "This is critical for exactly-once semantics"

## Troubleshooting

### Tests Failing with "Port Already in Use"

**Cause**: Another Kafka instance running on port 9092.

**Solution**:
```bash
# Stop other Kafka instances
docker-compose down

# Or change port in @EmbeddedKafka annotation
brokerProperties = {"listeners=PLAINTEXT://localhost:9093", "port=9093"}
```

### Tests Timing Out

**Cause**: Services not processing events fast enough.

**Solution**:
- Increase timeout in `await().atMost()` calls
- Check that all services are properly configured
- Verify Embedded Kafka is starting correctly

### Database Cleanup Issues

**Cause**: Data from previous tests interfering.

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

## Next Steps

- Add more edge case tests
- Add performance tests
- Add chaos engineering tests (simulate failures)
- Add contract tests (verify event schemas)

