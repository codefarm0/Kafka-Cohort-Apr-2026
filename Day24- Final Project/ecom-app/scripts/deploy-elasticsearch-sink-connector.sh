#!/bin/bash
# Elasticsearch sink: product CDC topic → index "products" (Phase 4).
#
# Confluent Elasticsearch Sink 14.x: index name = Kafka topic name (lowercased) — see
# ElasticsearchSinkTask#createIndexName(). topic.index.map is NOT used in this version.
# Do not use RegexRouter (topic-mutating SMT): ES sink 14+ rejects it.

CONNECTOR_NAME="product-es-sink"
KAFKA_CONNECT_URL="${KAFKA_CONNECT_URL:-http://localhost:8083}"

echo "Deploying Elasticsearch sink for index products..."

EXISTING=$(curl -s "$KAFKA_CONNECT_URL/connectors/$CONNECTOR_NAME")
if [ "$EXISTING" != "null" ] && [ -n "$EXISTING" ]; then
  echo "Deleting existing connector $CONNECTOR_NAME"
  curl -s -X DELETE "$KAFKA_CONNECT_URL/connectors/$CONNECTOR_NAME"
  sleep 2
fi

curl -s -X POST "$KAFKA_CONNECT_URL/connectors" \
  -H "Content-Type: application/json" \
  -d @- <<'EOF'
{
  "name": "product-es-sink",
  "config": {
    "connector.class": "io.confluent.connect.elasticsearch.ElasticsearchSinkConnector",
    "tasks.max": "1",
    "topics": "mysql-product-server.product_db.products",
    "connection.url": "http://elasticsearch:9200",
    "type.name": "_doc",
    "key.ignore": "true",
    "schema.ignore": "true",
    "write.method": "upsert",
    "behavior.on.null.values": "ignore",
    "transforms": "unwrap",
    "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
    "transforms.unwrap.drop.tombstones": "false",
    "transforms.unwrap.delete.handling.mode": "drop",
    "key.converter": "org.apache.kafka.connect.json.JsonConverter",
    "key.converter.schemas.enable": "false",
    "value.converter": "org.apache.kafka.connect.json.JsonConverter",
    "value.converter.schemas.enable": "false",
    "errors.tolerance": "all",
    "errors.log.enable": "true"
  }
}
EOF

echo ""
echo "Elasticsearch index (topic name, lowercased): mysql-product-server.product_db.products"
echo "Search Service is configured to use this index in application.yml."
echo "Optional alias: ./scripts/create-products-index-alias.sh"
echo "Status: curl -s $KAFKA_CONNECT_URL/connectors/$CONNECTOR_NAME/status"
