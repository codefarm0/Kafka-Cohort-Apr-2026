# Streams Dashboard Service

Kafka Streams app that **merges** `orders`, `payments`, and `deliveries`, maintains **durable counters** in a state store, and exposes them via **REST Interactive Queries** for the **ecom-web** Analytics page.

## Run (local)

1. Start Kafka (e.g. `docker compose up -d kafka` from repo root).
2. From this directory:

```bash
./gradlew bootRun
```

- **Port:** `8087`
- **REST:** `GET http://localhost:8087/api/dashboard/metrics`

## Configuration

| Property | Default | Notes |
|----------|---------|--------|
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Or `SPRING_KAFKA_BOOTSTRAP_SERVERS` |
| `spring.kafka.streams.application-id` | `streams-dashboard-v1` | New `application.id` → new state |
| `spring.kafka.streams.properties.auto.offset.reset` | `earliest` | Replays retained history on first run |
| `spring.kafka.streams.state-dir` | `${user.home}/.kafka-streams-dashboard` | Override with `KAFKA_STREAMS_STATE_DIR` |

## Topics & event types

Consumes (merged stream):

- `orders` — `com.ecommerce.order.placed`
- `payments` — `com.ecommerce.payment.processed`, `com.ecommerce.payment.failed`
- `deliveries` — `com.ecommerce.delivery.shipped`, `com.ecommerce.delivery.delivered`

Revenue (`revenueTotal` in JSON) sums `data.amount` on **payment processed** only.

## Limitations (training scope)

- **Cumulative counters**, not per-minute rates or windows. Reprocessing (offset reset, new app id, or replay) **adds again** — acceptable for demos; production would use windows, changelog compaction strategy, or an external store.
- Malformed JSON: **logged and skipped**; processing continues.

## Docker

From repo root:

```bash
docker compose up -d streams-dashboard
```

Requires the `kafka` service on `kafka:29092` (same network as other apps).
