# Implementation Guide

## Overview

This directory will contain the implementation code for all microservices.

**Hands-on:** Step-by-step **end-to-end demo** (product → Kafka Connect / Elasticsearch → search → order → payment → delivery) is in **[docs/E2E_SCENARIO_PRODUCT_TO_ORDER.md](docs/E2E_SCENARIO_PRODUCT_TO_ORDER.md)**. The implementation will follow the architecture defined in the `../architecture/` directory.

## Implementation Status

🚧 **Implementation Phase - Coming Soon**

This section will be populated during the implementation phase.

## Planned Structure

```
implementation/
├── order-service/          # Order Service implementation
├── payment-service/        # Payment Service implementation
├── inventory-service/      # Inventory Service implementation
├── shipping-service/       # Shipping Service implementation
├── notification-service/   # Notification Service implementation
├── streams-dashboard-service/  # Kafka Streams dashboard (topics → metrics API)
├── docker-compose.yml      # Infrastructure setup
├── scripts/                # Utility scripts
└── README.md              # This file
```

## Implementation Checklist

### Infrastructure Setup
- [ ] Docker Compose configuration
- [ ] Kafka cluster setup (KRaft mode)
- [ ] MySQL containers (with binlog enabled)
- [ ] Kafka Connect setup
- [ ] Debezium MySQL connector configuration
- [ ] Kafdrop setup
- [ ] New Relic agent configuration
- [ ] Network configuration

### Order Service
- [ ] Project setup (Spring Boot)
- [ ] Database schema and entities
- [ ] REST API endpoints
- [ ] Outbox pattern implementation (outbox table only, no poller)
- [ ] CloudEvents SDK integration
- [ ] Event consumers (CloudEvents format)
- [ ] Saga orchestration logic
- [ ] Unit tests
- [ ] Integration tests

### Debezium CDC Setup
- [ ] Configure MySQL binlog for CDC
- [ ] Deploy Kafka Connect with Debezium
- [ ] Configure Debezium MySQL connector with CloudEvents format
- [ ] Set up topic naming and routing
- [ ] Configure CloudEvents transformation
- [ ] Test CDC event publishing (CloudEvents format)

### Payment Service
- [ ] Project setup (Spring Boot)
- [ ] Database schema and entities
- [ ] CloudEvents SDK integration
- [ ] Event consumers (CloudEvents format)
- [ ] Payment processing logic
- [ ] Idempotency implementation
- [ ] Payment gateway integration (mock)
- [ ] Event producers (CloudEvents format)
- [ ] Unit tests
- [ ] Integration tests

### Inventory Service
- [ ] Project setup (Spring Boot)
- [ ] Database schema and entities
- [ ] CloudEvents SDK integration
- [ ] Event consumers (CloudEvents format)
- [ ] Inventory management logic
- [ ] Reservation system
- [ ] Idempotency implementation
- [ ] Event producers (CloudEvents format)
- [ ] Unit tests
- [ ] Integration tests

### Shipping Service
- [ ] Project setup (Spring Boot)
- [ ] Database schema and entities
- [ ] CloudEvents SDK integration
- [ ] Event consumers (CloudEvents format)
- [ ] Shipping label creation
- [ ] Idempotency implementation
- [ ] Shipping API integration (mock)
- [ ] Event producers (CloudEvents format)
- [ ] Unit tests
- [ ] Integration tests

### Notification Service
- [ ] Project setup (Spring Boot)
- [ ] Database schema and entities
- [ ] CloudEvents SDK integration
- [ ] Event consumers (CloudEvents format)
- [ ] Email service integration (mock)
- [ ] SMS service integration (mock)
- [ ] Idempotency implementation
- [ ] Event producers (CloudEvents format)
- [ ] Unit tests
- [ ] Integration tests

### Testing
- [ ] Unit tests for all services
- [ ] Integration tests
- [ ] End-to-end tests
- [ ] Performance tests
- [ ] Chaos tests

### Documentation
- [ ] API documentation
- [ ] Deployment guide
- [ ] Troubleshooting guide
- [ ] Development guide

## Implementation Order

1. **Infrastructure Setup** - Get Kafka, databases, and Kafka Connect running
2. **Debezium CDC Setup** - Configure Debezium connector for Order DB
3. **Order Service** - Core service with outbox pattern (no scheduled poller)
4. **Payment Service** - First downstream service
5. **Inventory Service** - Second downstream service
6. **Shipping Service** - Third downstream service
7. **Notification Service** - Final service
8. **Integration Testing** - End-to-end validation
9. **Documentation** - Complete all docs

## Key Implementation Guidelines

### Code Quality
- Follow Spring Boot best practices
- Use proper error handling
- Implement comprehensive logging
- Write unit and integration tests
- Follow clean code principles

### Kafka Best Practices
- Use transactional producers
- Implement idempotent consumers
- Proper error handling and retries
- Use Dead Letter Topics
- Maintain message ordering
- Use CloudEvents standard format for all events

### Database Best Practices
- Use transactions appropriately
- Implement proper indexing
- Use connection pooling
- Handle database failures gracefully
- Enable MySQL binlog for Debezium CDC
- Configure binlog format (ROW-based replication)

### Testing Best Practices
- High test coverage (>80%)
- Test happy paths and failure scenarios
- Use Testcontainers for integration tests
- Mock external services
- Test idempotency

## Getting Started

Once implementation begins:

1. Review architecture documents
2. Set up development environment
3. Start with infrastructure setup
4. Implement services one by one
5. Test each service thoroughly
6. Integrate and test end-to-end

## Resources

- [E2E scenario: product → ES → search → order](docs/E2E_SCENARIO_PRODUCT_TO_ORDER.md)
- [Architecture Overview](../architecture/01-architecture-overview.md)
- [Component Diagram](../architecture/02-component-diagram.md)
- [Sequence Diagrams](../architecture/03-sequence-diagrams.md)
- [Test Scenarios](../architecture/04-test-scenarios.md)

---

## Debezium CDC Configuration

### Overview

Instead of using a scheduled outbox poller, we use **Debezium Change Data Capture (CDC)** to capture database changes in real-time from the MySQL transaction log (binlog) and publish events immediately to Kafka.

### Key Benefits

- ✅ **Real-time processing**: Events published immediately after transaction commit
- ✅ **No polling overhead**: No scheduled tasks or database queries
- ✅ **Lower latency**: Events available in Kafka within milliseconds
- ✅ **Scalable**: Debezium handles high-throughput scenarios efficiently
- ✅ **Reliable**: Based on database transaction log, ensuring no events are missed

### Architecture

```
Order Service → Order DB (with outbox table)
                    ↓
              MySQL Binlog
                    ↓
            Debezium Connector
                    ↓
                 Kafka Topics
```

### MySQL Configuration

The Order DB MySQL instance must have binlog enabled:

```ini
# MySQL Configuration (my.cnf or docker-compose)
[mysqld]
server-id = 1
log-bin = mysql-bin
binlog_format = ROW
binlog_row_image = FULL
expire_logs_days = 7
```

### Debezium Connector Configuration

Example Debezium MySQL connector configuration (with CloudEvents format):

```json
{
  "name": "order-service-connector",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "tasks.max": "1",
    "database.hostname": "order-db",
    "database.port": "3306",
    "database.user": "debezium",
    "database.password": "debezium",
    "database.server.id": "184054",
    "database.server.name": "order-db-server",
    "database.include.list": "order_db",
    "table.include.list": "order_db.outbox_events",
    "database.history.kafka.bootstrap.servers": "kafka:9092",
    "database.history.kafka.topic": "order-db-schema-changes",
    "transforms": "outbox,cloudevents",
    "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
    "transforms.outbox.route.topic.replacement": "orders",
    "transforms.outbox.table.field.event.key": "aggregate_id",
    "transforms.outbox.table.field.event.timestamp": "created_at",
    "transforms.outbox.table.field.event.id": "id",
    "transforms.outbox.table.field.event.payload": "payload",
    "transforms.outbox.table.field.event.type": "event_type",
    "transforms.cloudevents.type": "io.debezium.transforms.CloudEventsConverter",
    "transforms.cloudevents.data.serialization.type": "cloud_events_json",
    "transforms.cloudevents.schema.enable": "false"
  }
}
```

**Note**: See the [CloudEvents Standard Implementation](#cloudevents-standard-implementation) section below for detailed CloudEvents configuration and usage.

### Outbox Table Schema

The outbox table structure for Debezium (CloudEvents format):

```sql
CREATE TABLE outbox_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    aggregate_id VARCHAR(255) NOT NULL,           -- Used as Kafka message key and CloudEvents subject
    event_type VARCHAR(255) NOT NULL,             -- CloudEvents type (e.g., com.ecommerce.order.placed)
    source VARCHAR(255) NOT NULL DEFAULT '/order-service',  -- CloudEvents source
    payload TEXT NOT NULL,                         -- CloudEvents data (JSON)
    correlation_id VARCHAR(255),                   -- Optional: for distributed tracing
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    -- Note: No status or published_at fields needed with Debezium
    -- Debezium automatically captures committed transactions
);
```

### Event Routing

Debezium's EventRouter transform automatically:
- Routes events to topics based on `event_type` field
- Uses `aggregate_id` as Kafka message key (for partition ordering)
- Extracts `payload` as the message value
- Handles event deduplication via transaction log

### Docker Compose Setup

Example docker-compose.yml additions:

```yaml
services:
  kafka-connect:
    image: debezium/connect:latest
    ports:
      - "8083:8083"
    environment:
      - BOOTSTRAP_SERVERS=kafka:9092
      - GROUP_ID=1
      - CONFIG_STORAGE_TOPIC=connect_configs
      - OFFSET_STORAGE_TOPIC=connect_offsets
      - STATUS_STORAGE_TOPIC=connect_statuses
    depends_on:
      - kafka
      - order-db

  order-db:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: order_db
      MYSQL_USER: debezium
      MYSQL_PASSWORD: debezium
    command: --server-id=1 --log-bin=mysql-bin --binlog-format=ROW --binlog-row-image=FULL
    volumes:
      - order-db-data:/var/lib/mysql
```

### Testing Debezium CDC

1. **Verify binlog is enabled**:
   ```sql
   SHOW VARIABLES LIKE 'log_bin';
   SHOW VARIABLES LIKE 'binlog_format';
   ```

2. **Create outbox event**:
   ```sql
   INSERT INTO outbox_events (aggregate_id, event_type, payload)
   VALUES ('order-123', 'OrderPlacedEvent', '{"orderId":"order-123",...}');
   COMMIT;
   ```

3. **Verify event in Kafka**:
   - Check Kafka topic (based on event_type)
   - Verify message key is aggregate_id
   - Verify message value is payload

### Monitoring

- **New Relic Integration**: Monitor all services and infrastructure
- Monitor Debezium connector status via Kafka Connect REST API
- Check connector metrics and lag in New Relic
- Monitor binlog position and replication lag
- Set up New Relic alerts for connector failures
- Track Kafka metrics (throughput, latency, consumer lag)
- Monitor application performance (APM) for all Spring Boot services

### Troubleshooting

**Issue**: Events not appearing in Kafka
- Check Debezium connector status
- Verify binlog is enabled and configured correctly
- Check connector logs for errors
- Verify table.include.list includes outbox_events

**Issue**: Duplicate events
- Debezium handles this via binlog position tracking
- Ensure proper connector restart handling
- Check offset storage topic

**Issue**: High latency
- Check binlog replication lag
- Monitor Kafka Connect worker resources
- Verify network connectivity

---

## CloudEvents Standard Implementation

### Overview

**CloudEvents** is a CNCF specification for describing event data in a common way. We adopt CloudEvents as the standard event format across all microservices.

**📚 For comprehensive CloudEvents documentation, see**: [Day 16: CloudEvents Deep Dive](../../day16-cloudevents-deep-dive.md)

**Reference**: [CloudEvents Specification](https://cloudevents.io/)

### Quick Reference

**Event Type Naming**: `com.ecommerce.<domain>.<action>`
- Examples: `com.ecommerce.order.placed`, `com.ecommerce.payment.processed`

**Required Attributes**: `id`, `source`, `type`, `specversion`

**Source Format**: `/order-service`, `/payment-service`, etc.

### Debezium CloudEvents Configuration

Configure Debezium to emit events in CloudEvents format:

```json
{
  "name": "order-service-connector",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "tasks.max": "1",
    "database.hostname": "order-db",
    "database.port": "3306",
    "database.user": "debezium",
    "database.password": "debezium",
    "database.server.id": "184054",
    "database.server.name": "order-db-server",
    "database.include.list": "order_db",
    "table.include.list": "order_db.outbox_events",
    "database.history.kafka.bootstrap.servers": "kafka:9092",
    "database.history.kafka.topic": "order-db-schema-changes",
    
    "transforms": "outbox,cloudevents",
    "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
    "transforms.outbox.route.topic.replacement": "${routedByValue}",
    "transforms.outbox.table.field.event.key": "aggregate_id",
    "transforms.outbox.table.field.event.timestamp": "created_at",
    "transforms.outbox.table.field.event.id": "id",
    "transforms.outbox.table.field.event.payload": "payload",
    "transforms.outbox.table.field.event.type": "event_type",
    
    "transforms.cloudevents.type": "io.debezium.transforms.CloudEventsConverter",
    "transforms.cloudevents.data.serialization.type": "cloud_events_json",
    "transforms.cloudevents.schema.enable": "false"
  }
}
```

### Outbox Table Schema for CloudEvents

Update the outbox table to support CloudEvents format:

```sql
CREATE TABLE outbox_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    aggregate_id VARCHAR(255) NOT NULL,           -- Used as Kafka message key and CloudEvents subject
    event_type VARCHAR(255) NOT NULL,             -- CloudEvents type (e.g., com.ecommerce.order.placed)
    source VARCHAR(255) NOT NULL DEFAULT '/order-service',  -- CloudEvents source
    payload TEXT NOT NULL,                         -- CloudEvents data (JSON)
    correlation_id VARCHAR(255),                   -- Optional: for distributed tracing
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Maven Dependencies


```xml
<dependency>
    <groupId>io.cloudevents</groupId>
    <artifactId>cloudevents-core</artifactId>
    <version>2.5.0</version>
</dependency>
<dependency>
    <groupId>io.cloudevents</groupId>
    <artifactId>cloudevents-json-jackson</artifactId>
    <version>2.5.0</version>
</dependency>
<dependency>
    <groupId>io.cloudevents</groupId>
    <artifactId>cloudevents-kafka</artifactId>
    <version>2.5.0</version>
</dependency>
```

### Basic CloudEvent Service

```java
package com.ecommerce.orderservice.event;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class CloudEventService {
    
    private static final String SOURCE_PREFIX = "/order-service";
    
    public CloudEvent createCloudEvent(String eventType, String subject, Object data) {
        return CloudEventBuilder.v1()
            .withId(UUID.randomUUID().toString())
            .withType(eventType)
            .withSource(URI.create(SOURCE_PREFIX))
            .withSubject(subject)
            .withTime(OffsetDateTime.now())
            .withDataContentType("application/json")
            .withData(serializeData(data))
            .build();
    }
    
    private byte[] serializeData(Object data) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsBytes(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event data", e);
        }
    }
}
```

### Kafka Configuration

**Producer**:
```java
@Bean
public ProducerFactory<String, CloudEvent> cloudEventProducerFactory() {
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, CloudEventSerializer.class);
    // ... other config
}
```

**Consumer**:
```java
@Bean
public ConsumerFactory<String, CloudEvent> cloudEventConsumerFactory() {
    configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, CloudEventDeserializer.class);
    // ... other config
}
```

### Example Event

```json
{
  "specversion": "1.0",
  "type": "com.ecommerce.order.placed",
  "source": "/order-service",
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "time": "2024-01-15T10:30:00Z",
  "datacontenttype": "application/json",
  "subject": "order-123",
  "data": {
    "orderId": "order-123",
    "customerId": "customer-456",
    "totalAmount": 99.99
  }
}
```

### Key Guidelines

1. **Event Types**: Use reverse DNS notation (`com.ecommerce.order.placed`)
2. **Source**: Use service name (`/order-service`)
3. **Subject**: Use business entity ID (order ID, payment ID)
4. **Correlation ID**: Include for distributed tracing
5. **Data Payload**: Keep focused and minimal

### Resources

- [Day 16: CloudEvents Deep Dive](../../day16-cloudevents-deep-dive.md) - Comprehensive guide
- [CloudEvents Specification](https://cloudevents.io/)
- [CloudEvents Java SDK](https://github.com/cloudevents/sdk-java)

---

## New Relic Monitoring Setup

### Overview

New Relic provides comprehensive Application Performance Monitoring (APM), infrastructure monitoring, and log aggregation for the entire e-commerce system.

### Key Features

- ✅ **APM**: Real-time application performance monitoring for all Spring Boot services
- ✅ **Infrastructure Monitoring**: Monitor Kafka, MySQL, and container metrics
- ✅ **Log Management**: Centralized log aggregation and search
- ✅ **Alerting**: Custom alerts for errors, performance degradation, and SLA violations
- ✅ **Distributed Tracing**: End-to-end transaction tracing across services
- ✅ **Kafka Monitoring**: Track consumer lag, throughput, and broker health

### New Relic Agent Configuration

#### Spring Boot Services

Add New Relic Java agent to each Spring Boot service:

**Maven Dependency** (pom.xml):
```xml
<dependency>
    <groupId>com.newrelic.agent.java</groupId>
    <artifactId>newrelic-java</artifactId>
    <version>8.0.0</version>
</dependency>
```

**Application Properties** (application.yml):
```yaml
newrelic:
  config:
    app_name: order-service
    license_key: ${NEW_RELIC_LICENSE_KEY}
    distributed_tracing:
      enabled: true
    application_logging:
      enabled: true
      forwarding:
        enabled: true
```

**Environment Variables**:
```bash
NEW_RELIC_LICENSE_KEY=your-license-key
NEW_RELIC_APP_NAME=order-service
```

#### Docker Compose Configuration

```yaml
services:
  order-service:
    image: order-service:latest
    environment:
      - NEW_RELIC_LICENSE_KEY=${NEW_RELIC_LICENSE_KEY}
      - NEW_RELIC_APP_NAME=order-service
      - NEW_RELIC_ENABLED=true
    volumes:
      - ./newrelic:/newrelic
```

### Infrastructure Monitoring

#### Kafka Monitoring

New Relic Infrastructure agent can monitor Kafka brokers:

```yaml
# Infrastructure agent configuration
integrations:
  - name: nri-kafka
    env:
      METRICS: true
      HOSTNAME: kafka
      PORT: 9092
```

#### MySQL Monitoring

Monitor database performance:

```yaml
integrations:
  - name: nri-mysql
    env:
      HOSTNAME: order-db
      PORT: 3306
      USERNAME: monitoring
      PASSWORD: ${MYSQL_MONITORING_PASSWORD}
```

### Metrics to Monitor

#### Application Metrics
- Request rate and latency
- Error rate
- Transaction duration
- Database query performance
- Kafka producer/consumer metrics

#### Infrastructure Metrics
- Kafka broker health
- Consumer lag
- Topic throughput
- Database connection pool
- Container CPU/memory usage

#### Business Metrics
- Orders processed per minute
- Payment success rate
- Inventory reservation success rate
- Average order processing time

### Alerting Configuration

Example New Relic alerts:

1. **High Error Rate**
   - Condition: Error rate > 5% for 5 minutes
   - Notification: Email, Slack, PagerDuty

2. **High Consumer Lag**
   - Condition: Kafka consumer lag > 1000 messages
   - Notification: Team channel

3. **Service Down**
   - Condition: Service health check fails
   - Notification: On-call engineer

4. **Slow Transactions**
   - Condition: P95 latency > 2 seconds
   - Notification: Development team

### Dashboard Setup

Create custom dashboards for:
- **System Overview**: All services health and metrics
- **Kafka Dashboard**: Topics, consumer groups, throughput
- **Order Processing**: End-to-end order flow metrics
- **Error Dashboard**: Error rates and types by service
- **Performance Dashboard**: Latency and throughput trends

### Log Integration

Forward application logs to New Relic:

```yaml
# Logback configuration (logback-spring.xml)
<appender name="NEW_RELIC" class="com.newrelic.logging.logback.NewRelicLoggingAppender">
    <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
        <layout class="com.newrelic.logging.logback.NewRelicJsonLayout"/>
    </encoder>
</appender>
```

### Distributed Tracing

Enable distributed tracing to track requests across services:

```yaml
newrelic:
  config:
    distributed_tracing:
      enabled: true
    span_events:
      enabled: true
```

### Best Practices

1. **Service Naming**: Use consistent naming convention (e.g., `order-service`, `payment-service`)
2. **Custom Attributes**: Add business context to transactions (orderId, customerId)
3. **Error Tracking**: Ensure all exceptions are properly logged and tracked
4. **Performance Baselines**: Establish performance baselines for alerting
5. **Regular Reviews**: Review dashboards and alerts regularly

### Testing New Relic Integration

1. **Verify Agent Connection**:
   - Check New Relic UI for service appearance
   - Verify license key is correct

2. **Test Metrics Collection**:
   - Generate some traffic
   - Verify metrics appear in New Relic

3. **Test Alerting**:
   - Trigger a test alert
   - Verify notification delivery

4. **Test Distributed Tracing**:
   - Make a request that spans multiple services
   - Verify trace appears in New Relic

---

**Implementation will begin in the next phase! 🚀**

