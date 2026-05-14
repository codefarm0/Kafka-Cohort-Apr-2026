# Event Sourcing Implementation

This document explains the **Event Sourcing** implementation added to the saga pattern simulation project. For theoretical background and concepts, refer to [Day 17: Event Sourcing & Event Replays](../../day17-event-sourcing-and-event-replays.md).

## Overview

Event Sourcing has been added as an **incremental change** to the existing saga pattern. All services continue to work as before, with the addition of:

1. **Event Store**: A new `order-events` topic that stores all events
2. **Event Replay**: Ability to replay events and reconstruct order state
3. **Time Travel**: Query order state at any point in time

## Architecture

### Dual Publishing Pattern

Each service now publishes events to **two topics**:

1. **Saga Topics** (existing): `orders`, `payments`, `inventory` - for saga orchestration
2. **Event Store** (new): `order-events` - for event sourcing and replay

```
Order Service
  ├─> orders topic (saga)
  └─> order-events topic (event store)

Payment Service
  ├─> payments topic (saga)
  └─> order-events topic (event store)

Inventory Service
  ├─> inventory topic (saga)
  └─> order-events topic (event store)
```

### Event Store Topic Configuration

The `order-events` topic is configured with:

- **Retention Policy**: `DELETE` (NOT compaction) - keeps all events
- **Retention Period**: Forever (Long.MAX_VALUE)
- **Partitioning**: By `orderId` (ensures ordering)
- **Partitions**: 6

**Critical**: We use `DELETE` retention, NOT `COMPACT`. Log compaction would delete old events for the same key, losing history!

## Code Changes

### 1. EventStoreService

**New Service**: `in.codefarm.saga.eventsourcing.EventStoreService`

```java
@Service
public class EventStoreService {
    public void appendEvent(String orderId, EventWrapper<?> event) {
        kafkaTemplate.send("order-events", orderId, event);
    }
}
```

**Purpose**: Centralized service to append events to the event store.

**Key Points**:
- Uses `orderId` as Kafka key (ensures all events for same order in same partition)
- Events are append-only (immutable)
- Non-blocking (async publishing)

### 2. Updated Event Producers

All event producers now also publish to the event store:

#### OrderEventProducer

**Change**: Added `EventStoreService` dependency and call to `appendEvent()`:

```java
// Before: Only published to saga topic
kafkaTemplate.send(ORDERS_TOPIC, payload.orderId(), event);

// After: Also publishes to event store
kafkaTemplate.send(ORDERS_TOPIC, payload.orderId(), event);
eventStoreService.appendEvent(payload.orderId(), event);  // NEW
```

**Files Modified**:
- `OrderEventProducer.java` - Added event store publishing for `OrderPlacedEvent` and `OrderCancelledEvent`

#### PaymentEventProducer

**Change**: Added event store publishing:

```java
// After processing payment
kafkaTemplate.send(PAYMENT_TOPIC, payload.orderId(), event);
eventStoreService.appendEvent(payload.orderId(), event);  // NEW
```

**Files Modified**:
- `PaymentEventProducer.java` - Added event store publishing for `PaymentProcessedEvent` and `PaymentRefundedEvent`

#### InventoryEventProducer

**Change**: Added event store publishing:

```java
// After reserving inventory
kafkaTemplate.send(TOPIC_NAME, payload.orderId(), event);
eventStoreService.appendEvent(payload.orderId(), event);  // NEW
```

**Files Modified**:
- `InventoryEventProducer.java` - Added event store publishing for `InventoryReservedEvent`

### 3. Topic Configuration

**File**: `KafkaConfig.java`

**Change**: Added topic bean for `order-events`:

```java
@Bean
public NewTopic orderEventsTopic() {
    return TopicBuilder.name("order-events")
        .partitions(6)
        .replicationFactor(1)
        .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Long.MAX_VALUE))
        .config(TopicConfig.CLEANUP_POLICY_CONFIG, "delete")  // DELETE, NOT compact!
        .build();
}
```

### 4. OrderState (State Reconstruction)

**New Class**: `in.codefarm.saga.eventsourcing.OrderState`

**Purpose**: Represents the reconstructed state of an order from events.

**Key Methods**:
- `applyOrderPlaced()` - Applies OrderPlacedEvent
- `applyPaymentProcessed()` - Applies PaymentProcessedEvent
- `applyInventoryReserved()` - Applies InventoryReservedEvent
- `applyOrderCancelled()` - Applies OrderCancelledEvent
- `applyPaymentRefunded()` - Applies PaymentRefundedEvent

**How it works**:
1. Start with empty state
2. Replay events in order
3. Apply each event to update state
4. Return final state

### 5. OrderReplayService

**New Service**: `in.codefarm.saga.eventsourcing.OrderReplayService`

**Purpose**: Replays events from event store to reconstruct order state.

**Key Methods**:

#### `replayOrder(String orderId)`

Replays all events for a specific order:

```java
OrderState state = replayService.replayOrder("order-123");
// Returns: Complete order state with all fields populated
```

**How it works**:
1. Create Kafka consumer
2. Assign all partitions
3. Seek to beginning
4. Poll for events
5. Filter events by `orderId` (Kafka key)
6. Apply each event to state
7. Return reconstructed state

#### `replayToTimestamp(String orderId, LocalDateTime timestamp)`

Replays events up to a specific point in time:

```java
OrderState state = replayService.replayToTimestamp(
    "order-123", 
    LocalDateTime.parse("2024-01-15T10:30:00")
);
// Returns: Order state as it was at that timestamp
```

**Use Cases**:
- Audit queries: "What was the order status on Jan 15th?"
- Debugging: "What was the state when the bug occurred?"
- Compliance: "Show me the order state at time of payment"

### 6. ReplayController

**New Controller**: `in.codefarm.saga.eventsourcing.ReplayController`

**Purpose**: REST endpoints for replay operations.

**Endpoints**:

#### GET `/api/replay/order/{orderId}`

Replay all events for an order:

```bash
curl http://localhost:8088/api/replay/order/order-123
```

**Response**:
```json
{
  "orderId": "order-123",
  "customerId": "customer-456",
  "productId": "product-789",
  "quantity": 2,
  "totalAmount": 99.99,
  "status": "INVENTORY_RESERVED",
  "paymentId": "pay-123",
  "paymentStatus": "SUCCESS",
  "inventoryReservationId": "inv-456",
  "inventoryStatus": "RESERVED",
  "eventHistory": ["OrderPlaced", "PaymentProcessed", "InventoryReserved"]
}
```

#### GET `/api/replay/order/{orderId}/at?timestamp=2024-01-15T10:30:00`

Replay events up to a specific timestamp:

```bash
curl "http://localhost:8088/api/replay/order/order-123/at?timestamp=2024-01-15T10:30:00"
```

**Response**:
```json
{
  "orderId": "order-123",
  "status": "PAYMENT_COMPLETED",
  "eventHistory": ["OrderPlaced", "PaymentProcessed"],
  "replayedUpTo": "2024-01-15T10:30:00"
}
```

## How It Works

### Event Flow

1. **Order Placed**:
   ```
   OrderService → OrderPlacedEvent
     ├─> orders topic (saga)
     └─> order-events topic (event store)
   ```

2. **Payment Processed**:
   ```
   PaymentService → PaymentProcessedEvent
     ├─> payments topic (saga)
     └─> order-events topic (event store)
   ```

3. **Inventory Reserved**:
   ```
   InventoryService → InventoryReservedEvent
     ├─> inventory topic (saga)
     └─> order-events topic (event store)
   ```

### Replay Flow

1. **Request Replay**:
   ```
   GET /api/replay/order/order-123
   ```

2. **ReplayService**:
   - Creates Kafka consumer
   - Seeks to beginning of `order-events` topic
   - Polls for events with key = `order-123`
   - Applies each event to `OrderState`

3. **State Reconstruction**:
   ```
   Empty State
     → applyOrderPlaced() → status: PLACED
     → applyPaymentProcessed() → status: PAYMENT_COMPLETED
     → applyInventoryReserved() → status: INVENTORY_RESERVED
   ```

4. **Return State**:
   - Complete order state with all fields
   - Event history showing sequence of events

## Storage Considerations

### Kafka Topic Growth

The `order-events` topic will grow over time as events accumulate. This is expected and necessary for event sourcing.

**Storage Strategy**:

1. **Short-term** (0-90 days): Keep in Kafka
   - Fast replay
   - Easy access

2. **Long-term** (90+ days): Archive to database
   - Create snapshots periodically
   - Store old events in database
   - Replay from snapshot + recent events

**Future Enhancement**: Implement snapshot strategy (see Day 17 document).

### Retention Policy

**Current**: `DELETE` retention with `Long.MAX_VALUE` (keep forever)

**Why NOT compaction?**:
- Log compaction keeps only the latest value per key
- Would delete old events for the same `orderId`
- Loses history (defeats the purpose of event sourcing)

## Testing

### 1. Place an Order

```bash
curl -X POST http://localhost:8088/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-123",
    "productId": "product-456",
    "quantity": 2,
    "totalAmount": 99.99
  }'
```

### 2. Verify Events in Event Store

```bash
docker exec -it kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic order-events \
  --from-beginning \
  --property print.key=true
```

**Expected Output**:
```
order-123    {"metadata":{"eventType":"OrderPlaced",...},"payload":{...}}
order-123    {"metadata":{"eventType":"PaymentProcessed",...},"payload":{...}}
order-123    {"metadata":{"eventType":"InventoryReserved",...},"payload":{...}}
```

### 3. Replay Order

```bash
curl http://localhost:8088/api/replay/order/order-123
```

**Expected**: Complete order state with all events applied.

### 4. Replay to Timestamp

```bash
curl "http://localhost:8088/api/replay/order/order-123/at?timestamp=2024-01-15T10:30:00"
```

**Expected**: Order state at that point in time.

## Key Design Decisions

### 1. Dual Publishing

**Why**: Maintain backward compatibility while adding event sourcing.

**Alternative**: Could have changed all services to only publish to event store, but that would break existing saga flow.

### 2. OrderId as Key

**Why**: Ensures all events for same order are in same partition, maintaining order.

**Benefit**: Easy to replay events for a specific order.

### 3. DELETE Retention

**Why**: Need to keep all events for complete history.

**Trade-off**: Storage grows, but enables time travel and audit trails.

### 4. Separate Replay Consumer

**Why**: Replay operations need to seek to beginning, which is different from normal consumption.

**Benefit**: Doesn't interfere with saga consumers.

## Future Enhancements

1. **Snapshots**: Periodically save state to database for faster replay
2. **Event Archiving**: Move old events to cold storage
3. **Parallel Replay**: Replay multiple partitions in parallel
4. **Event Versioning**: Handle schema evolution
5. **Read Models**: Build optimized read models from events

## Summary

Event Sourcing has been added with **minimal, incremental changes**:

✅ **New Components**:
- `EventStoreService` - Publishes to event store
- `OrderState` - State reconstruction
- `OrderReplayService` - Event replay logic
- `ReplayController` - REST API for replay

✅ **Modified Components**:
- `OrderEventProducer` - Added event store publishing
- `PaymentEventProducer` - Added event store publishing
- `InventoryEventProducer` - Added event store publishing
- `KafkaConfig` - Added topic configuration

✅ **No Breaking Changes**:
- All existing saga functionality continues to work
- Event sourcing is additive, not replacement

For theoretical background, patterns, and best practices, see [Day 17: Event Sourcing & Event Replays](../../day17-event-sourcing-and-event-replays.md).

