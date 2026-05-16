# Search Service (Phase 4)

Read-only **Elasticsearch** queries for the product CDC index (populated by Kafka Connect from Debezium).

**Index name:** Confluent Elasticsearch Sink **14.x** uses the Kafka topic name (lowercased) as the ES index — default here is `mysql-product-server.product_db.products`. Optional alias `products` → see `scripts/create-products-index-alias.sh`.

## API

`GET /api/search?q=keyword&category=electronics&from=0&size=20`

- `q` optional (omit or empty = match_all)
- `category` optional filter on `category_id`
- `from` / `size` pagination (size capped at 100)

## Config

- `search.elasticsearch.url` (default `http://localhost:9200`)
- `search.elasticsearch.index` (default `mysql-product-server.product_db.products` — matches Kafka topic / ES sink 14 behavior)
- `search.elasticsearch.debezium-envelope` (default `true`) — query and map `_source.after.*` because CDC documents are Debezium envelopes; set `false` only if you index flat product documents

Docker profile: `application-docker.yml` uses `http://elasticsearch:9200`.

## Run

Requires Elasticsearch with indexed documents (see product-service README and connector scripts).

```bash
./gradlew bootRun
```

Port **8085**.
