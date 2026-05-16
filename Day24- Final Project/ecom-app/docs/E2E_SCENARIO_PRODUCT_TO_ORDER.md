# End-to-end scenario: Product → Elasticsearch → Search → Order → Payment → Delivery

This walkthrough runs **one coherent story** on your machine:

1. **Create/update a product** in **Product Service** (MySQL `product_db`).
2. **CDC** streams row changes to Kafka (**Debezium**), then **Kafka Connect** writes to **Elasticsearch** (index name = Kafka topic, e.g. `mysql-product-server.product_db.products`).
3. **Search Service** queries ES and returns hits for the React shop or `curl`.
4. **Place an order** via **Order Service** (Redis reservation + Kafka `orders` topic).
5. **Payment Service** consumes `orders`, charges (mock), publishes `payments`.
6. **Shipping / Delivery Service** creates **AWAITING_SHIPMENT**; you **mark SHIPPED / DELIVERED** via REST (or the **ecom-web** delivery page).

```
Product API → MySQL → Debezium → Kafka topic → ES Sink → Elasticsearch
                                                                  ↑
                                                          Search Service ← Shop UI / curl

Order API → MySQL + Redis reserve → Kafka `orders` → Payment → `payments` → Order + Delivery updates → Kafka `deliveries`
```

---

## Prerequisites

| Requirement | Notes |
|-------------|--------|
| **Docker Desktop** (or Docker Engine + Compose v2) | Enough RAM/CPU for Kafka + ES + several MySQL instances (~6–8 GB RAM comfortable). |
| **JDK** | **21+** (several services use modern Gradle/Spring; **25** if your `build.gradle` toolchains ask for it). |
| **Node.js 18+** | Only if you use **`ecom-web`** (`npm run dev`). |
| **curl** | For connector deploy scripts and API checks. |
| **bash** | Scripts under `scripts/` are bash (macOS/Linux; on Windows use **Git Bash** or **WSL**). |

---

## Service map (ports)

| What | Host port | Purpose in this scenario |
|------|-----------|---------------------------|
| **Kafka** | `9092` | Brokers; apps use `localhost:9092`. |
| **Redis** | `6379` | Order reservation + idempotency. |
| **Elasticsearch** | `9200` | Product CDC index (default `mysql-product-server.product_db.products`). |
| **Kibana** (optional) | `5601` | Inspect ES data. |
| **Kafka Connect** | `8083` | Debezium + Elasticsearch sink REST API. |
| **Kafdrop** (optional) | `9000` | Browse Kafka topics. |
| **product-db** | `3308` | Catalog (`product_db`). |
| **order-db** | `3306` | Orders. |
| **payment-db** | `3307` | Payments. |
| **shipping-db** | `3309` | Deliveries / shipments. |
| **notification-db** | `3310` | (Optional) notification records. |
| **Mailpit** (optional) | SMTP `1025`, UI `8025` | Catch emails from notification-service. |
| **product-service** | `8082` | CRUD products / inventory. |
| **search-service** | `8085` | `GET /api/search` → ES. |
| **order-service** | `8080` | `POST/GET /api/orders`. |
| **payment-service** | `8081` | Consumes `orders`, produces `payments`. |
| **shipping-service** | `8084` | `GET/PATCH /api/deliveries`. |
| **notification-service** | `8086` | (Optional) email on events. |
| **streams-dashboard-service** (optional) | `8087` | Kafka Streams merged `orders`/`payments`/`deliveries` → `GET /api/dashboard/metrics` (see `streams-dashboard-service/README.md`). |
| **ecom-web** (optional) | `5173` | Vite dev server; proxies `/api/*` to backends (includes `/api/dashboard` → `8087`). |

**Analytics UI:** With **ecom-web** and **streams-dashboard-service** running, open **Analytics → Kafka metrics** (`/analytics`) to poll dashboard metrics.

---

## Part A — Infrastructure & Kafka Connect

All commands assume repository root: **`ecom-app/`** (where `docker-compose.yml` lives).

### A1. Build the Connect image (first time or after Dockerfile changes)

The image adds the **Elasticsearch sink** plugin on top of **Debezium Connect**:

```bash
docker compose build kafka-connect
```

### A2. Start Docker dependencies

Minimum for **product → ES → search → order → payment → delivery**:

```bash
docker compose up -d kafka redis elasticsearch product-db order-db payment-db shipping-db kafka-connect
```

Recommended extras (UI + optional notifications):

```bash
docker compose up -d kibana kafdrop mailpit notification-db
```

Wait until **healthy** (especially `kafka`, `elasticsearch`, `kafka-connect`):

```bash
docker compose ps
```

### A3. Register Kafka Connect connectors

**Order matters:** Debezium first (creates the CDC topic), then the ES sink.

```bash
chmod +x scripts/deploy-debezium-product-connector.sh scripts/deploy-elasticsearch-sink-connector.sh

./scripts/deploy-debezium-product-connector.sh
./scripts/deploy-elasticsearch-sink-connector.sh
```

Check status:

```bash
curl -s http://localhost:8083/connectors | jq .
curl -s http://localhost:8083/connectors/product-db-debezium/status | jq .
curl -s http://localhost:8083/connectors/product-es-sink/status | jq .
```

- CDC topic name: **`mysql-product-server.product_db.products`**
- **Elasticsearch index name:** Confluent **Elasticsearch Sink 14.x** does **not** use `topic.index.map`. It writes to an index whose name equals the **Kafka topic name** (lowercased): **`mysql-product-server.product_db.products`**. (RegexRouter was removed — it’s incompatible with this connector.)

### A4. Confirm Elasticsearch has data

Seeded products exist from `scripts/init-product-db.sql` (`product-1` … `product-3`). After connectors are **RUNNING**, query the **real** index name:

```bash
IDX="mysql-product-server.product_db.products"
curl -s "http://localhost:9200/${IDX}/_search?size=5&pretty" | head -80
```

**Search Service** is configured to use this same index in `search-service/src/main/resources/application.yml` (`search.elasticsearch.index`).

Optional: create a short alias `products` → that index (then you *could* set `search.elasticsearch.index=products`):

```bash
chmod +x scripts/create-products-index-alias.sh
./scripts/create-products-index-alias.sh
```

You can also open **Kibana** → Dev Tools (`http://localhost:5601`) and run:

```json
GET mysql-product-server.product_db.products/_search
{
  "query": { "match_all": {} },
  "size": 5
}
```

---

## Part B — Run Spring Boot services

Open **separate terminals** for each (or use your IDE). Defaults use `localhost` brokers/DBs from each service’s `application.yml`.

```bash
# Terminal 1 — catalog + inventory API
cd product-service && ./gradlew bootRun

# Terminal 2 — search (reads ES)
cd search-service && ./gradlew bootRun

# Terminal 3 — orders + Redis reservation + Kafka producers/consumers
cd order-service && ./gradlew bootRun

# Terminal 4 — payment (consumes orders topic)
cd payment-service && ./gradlew bootRun

# Terminal 5 — delivery / shipment API + Kafka deliveries topic
cd shipping-service && ./gradlew bootRun
```

**Optional — notifications (SMTP → Mailpit):**

```bash
cd notification-service && ./gradlew bootRun
```

Ensure **Mailpit** is up if you want to see emails: `http://localhost:8025`.

---

## Part C — Scenario steps (happy path)

### Step 1 — Add a new product (Product Service)

This inserts into **MySQL**; Debezium → Kafka → ES sink updates the **`mysql-product-server.product_db.products`** index in Elasticsearch.

```bash
curl -s -X POST http://localhost:8082/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "demo-laptop-001",
    "sku": "LAP-DEMO-01",
    "productName": "Training Laptop 14",
    "description": "End-to-end demo product for Kafka training course",
    "categoryId": "electronics",
    "price": 899.00,
    "quantity": 25
  }' | jq .
```

Wait **a few seconds** for CDC + sink lag, then verify in ES (search by `product_id`):

```bash
curl -s "http://localhost:9200/mysql-product-server.product_db.products/_search?q=demo-laptop-001&pretty"
```

### Step 2 — Search (Search Service)

```bash
curl -s "http://localhost:8085/api/search?q=Training&size=10" | jq .
```

You should see a hit with `productId` **`demo-laptop-001`** (and `availableQuantity` if the field synced).

### Step 3 — Place an order (Order Service)

Use the **same** `productId` and **current** `unitPrice` / name as in catalog (order line is validated against product service).

```bash
ORDER_JSON=$(curl -s -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-demo-1",
    "shippingAddress": "99 Event Driven Ave, Kafka City",
    "items": [
      {
        "productId": "demo-laptop-001",
        "productName": "Training Laptop 14",
        "quantity": 1,
        "unitPrice": 899.00
      }
    ]
  }')

echo "$ORDER_JSON" | jq .
ORDER_ID=$(echo "$ORDER_JSON" | jq -r .id)
echo "ORDER_ID=$ORDER_ID"
```

### Step 4 — Watch order status (payment saga)

Payment service consumes **`orders`** and publishes **`payments`**. Order service updates status from those events.

Poll until you see **`PAYMENT_SUCCEEDED`** (or `PAYMENT_FAILED` if mock fails — retry with a new order if needed):

```bash
curl -s "http://localhost:8080/api/orders/$ORDER_ID" | jq .
```

### Step 5 — Delivery progression (Shipping Service)

List rows waiting to ship:

```bash
curl -s "http://localhost:8084/api/deliveries?status=AWAITING_SHIPMENT" | jq .
```

Mark **shipped**, then **delivered** (same `orderId` as above):

```bash
curl -s -X PATCH "http://localhost:8084/api/deliveries/$ORDER_ID/status" \
  -H "Content-Type: application/json" \
  -d '{"status":"SHIPPED"}' | jq .

curl -s -X PATCH "http://localhost:8084/api/deliveries/$ORDER_ID/status" \
  -H "Content-Type: application/json" \
  -d '{"status":"DELIVERED"}' | jq .
```

Refresh order status:

```bash
curl -s "http://localhost:8080/api/orders/$ORDER_ID" | jq .
```

You should see statuses aligned with **LLD** (e.g. shipped / delivered after delivery events are processed).

### Step 6 — Optional: React UI (`ecom-web`)

```bash
cd ecom-web
npm install
npm run dev
```

Open **`http://localhost:5173`**: **Shop** (search), **Cart**, **Checkout**, **Order** lookup, **Admin** products, **Delivery** dashboard — all use the same APIs as above (Vite **proxy** to local ports).

---

## Troubleshooting

| Symptom | What to check |
|---------|----------------|
| Connect build fails | See `docker/kafka-connect/README.md` (Maven fetch from Confluent public repo). |
| Connector `FAILED` | `docker compose logs kafka-connect --tail 200` and Connect REST `.../status`. |
| ES index empty | Debezium snapshot finished? Topic `mysql-product-server.product_db.products` has messages? (Kafdrop **9000**). |
| Search returns nothing | Index name matches **`search.elasticsearch.index`** (default long CDC name); field names snake_case (`product_name`, `product_id`, …). |
| Order create fails | Product exists in **product-service**, stock sufficient, **Redis** up, **prices** match. |
| Stays `PENDING` | **payment-service** running and connected to **Kafka** `localhost:9092`. |
| No delivery row | Payment must **succeed**; shipping service creates shipment on **payment processed** event. |

---

## Quick checklist

- [ ] `docker compose build kafka-connect` (once)
- [ ] Core containers up: **kafka, redis, elasticsearch, product-db, order-db, payment-db, shipping-db, kafka-connect**
- [ ] `./scripts/deploy-debezium-product-connector.sh`
- [ ] `./scripts/deploy-elasticsearch-sink-connector.sh`
- [ ] `curl localhost:9200/mysql-product-server.product_db.products/_search` returns hits (or alias `products` if you ran `create-products-index-alias.sh`)
- [ ] **product-service, search-service, order-service, payment-service, shipping-service** running
- [ ] POST product → search → POST order → poll order → PATCH delivery statuses

---

## Related docs

- `docker/kafka-connect/README.md` — why Connect is custom; ES image is official.
- `ecom-web/README.md` — frontend, Node version, npm registry (corporate `~/.npmrc`).
- `ecommerce-final-project-hld-lld.md` — architecture (HLD/LLD).
