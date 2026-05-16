# Product Service (Phase 4)

Catalog + stock in **MySQL** (`product_db`). Replaces the former **inventory-service** module.

## API

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/products` | List products |
| GET | `/api/products/{productId}` | By id |
| POST | `/api/products` | Create (body: `productId`, `productName`, `price`, `quantity`, optional `sku`, `description`, `categoryId`) |
| PUT | `/api/products/{productId}` | Full replace |
| DELETE | `/api/products/{productId}` | Delete |
| PUT | `/api/products/{productId}/quantity` | Set available quantity |
| POST | `/api/inventory/deduct` | Deduct after payment (order-service) |

## Local run

- DB: `localhost:3308`, database `product_db`, user `root` / `rootpassword` (see `application.yml`).
- Docker: use **`product-db`** from repo root `docker-compose.yml`, then `SPRING_PROFILES_ACTIVE=docker`.

## CDC → Elasticsearch

1. Build Connect image: `docker compose build kafka-connect` (pulls ES sink JARs from Confluent’s public Maven repo via Maven; needs network). See `docker/kafka-connect/README.md`.
2. `./scripts/deploy-debezium-product-connector.sh`
3. `./scripts/deploy-elasticsearch-sink-connector.sh`
4. Search: **search-service** `GET http://localhost:8085/api/search?q=...`
