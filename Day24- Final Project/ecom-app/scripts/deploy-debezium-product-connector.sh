#!/bin/bash
# Debezium MySQL connector for product_db.products → Kafka (Phase 4).
# Prerequisites: docker compose up (product-db, kafka, kafka-connect built image).
#
# decimal.handling.mode=string — DECIMAL columns appear as readable strings in JSON (e.g. "29.99").
# Default "precise" uses Kafka Connect Decimal logical type, which JsonConverter often encodes as
# base64 text (e.g. "C7c=") in the topic — confusing in Kibana/Kafka UI but lossless.

CONNECTOR_NAME="product-db-debezium"
KAFKA_CONNECT_URL="${KAFKA_CONNECT_URL:-http://localhost:8083}"

echo "Deploying Debezium connector for product_db..."

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
  "name": "product-db-debezium",
  "config": {
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "tasks.max": "1",
    "snapshot.mode": "initial",
    "database.hostname": "product-db",
    "database.port": "3306",
    "database.user": "debezium",
    "database.password": "debezium",
    "database.server.id": "285545",
    "database.server.name": "mysql-product-server",
    "database.include.list": "product_db",
    "table.include.list": "product_db.products",
    "topic.prefix": "mysql-product-server",
    "schema.history.internal.kafka.bootstrap.servers": "kafka:29092",
    "schema.history.internal.kafka.topic": "mysql-product-schema-history",
    "database.history.store.only.monitored.tables.ddl": "true",
    "decimal.handling.mode": "string",
    "key.converter": "org.apache.kafka.connect.json.JsonConverter",
    "key.converter.schemas.enable": "false",
    "value.converter": "org.apache.kafka.connect.json.JsonConverter",
    "value.converter.schemas.enable": "false"
  }
}
EOF

echo ""
echo "Topic will be: mysql-product-server.product_db.products"
echo "Status: curl -s $KAFKA_CONNECT_URL/connectors/$CONNECTOR_NAME/status"
