# Day 6 Demo: Interactive Queries — REST over State Stores

Stateful aggregations (running total per customer, count per category) with **Interactive Queries**: state stores are exposed via REST so dashboards and APIs can read current state directly from RocksDB. Same pipeline as Day 4, plus REST endpoints that query the stores.

## Prerequisites

- Java 25 (or adjust in `build.gradle`)
- Kafka at `localhost:9092`

## Topics

- **orders** — input (key = any, value = JSON with `customerId`, `totalAmount`, optional `categoryId`)
- **customer-order-totals** — output (key = customerId, value = Double = running total)
- **orders-per-category** — output (key = categoryId, value = Long = count)

## Run

```bash
./gradlew bootRun
```

App runs on port **8084**.

### Dashboard (Thymeleaf)

Open **http://localhost:8084/**.

- **Simulate (client):** Send one order or **bulk** orders. Orders go to input topic only.
- **From output topics:** Data consumed from `customer-order-totals` and `orders-per-category` (same as Day 4).
- **From state store (IQ):** Current state read via **Interactive Queries** from the same RocksDB stores the topology uses. Shown when Kafka Streams is RUNNING.
- **Alerts:** From processed results (threshold crossing).

## Endpoints

| Endpoint | Description |
|----------|-------------|
| `POST /api/orders` | Send an order event. Body: JSON with `customerId`, `totalAmount`, optional `categoryId`. |
| `GET /api/store/customer-totals` | Interactive Query: all customer totals from state store. `?key=...` for one key. |
| `GET /api/store/category-counts` | Interactive Query: all category counts from state store. `?key=...` for one key. |
| `GET /api/streams/topology` | Topology JSON + sub-topologies |
| `GET /api/streams/topology/visual` | Mermaid diagram of topology |
| `GET /actuator/health` | Health |

IQ endpoints return **503** with `store_not_ready` when Kafka Streams is not in RUNNING state.

## Try it

```bash
# Send orders
curl -X POST http://localhost:8084/api/orders -H "Content-Type: application/json" \
  -d '{"customerId":"c1","totalAmount":100.0,"categoryId":"electronics"}'

# After streams is RUNNING: query state store via REST
curl -s http://localhost:8084/api/store/customer-totals
curl -s "http://localhost:8084/api/store/customer-totals?key=c1"
curl -s http://localhost:8084/api/store/category-counts
```

## Config

- `app.kafka.input-topic`, `customer-totals-topic`, `orders-per-category-topic`
- **`state.dir`** — `${user.dir}/kafka-streams-state` (stores under project root)
- Alert thresholds: `app.dashboard.alert.total-threshold`, `category-count-threshold`

## Next

End of Kafka Streams syllabus. Days 1–6 cover fundamentals, stateless transforms, windowing, stateful aggregations, joins, and Interactive Queries + deployment.
