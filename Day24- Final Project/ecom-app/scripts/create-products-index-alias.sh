#!/bin/bash
# Optional: alias short name "products" -> actual CDC index (Confluent ES sink 14 uses topic name as index).
# Run after the ES index exists and search-service can use index: products if you set it so.

ES_URL="${ELASTICSEARCH_URL:-http://localhost:9200}"
REAL_INDEX="mysql-product-server.product_db.products"
ALIAS="products"

echo "Creating alias ${ALIAS} -> ${REAL_INDEX} on ${ES_URL}"

curl -s -X POST "${ES_URL}/_aliases" \
  -H "Content-Type: application/json" \
  -d "{
  \"actions\": [
    { \"add\": { \"index\": \"${REAL_INDEX}\", \"alias\": \"${ALIAS}\" } }
  ]
}" | jq .

echo "Then you may set search.elasticsearch.index=products in search-service if you prefer the short name."
