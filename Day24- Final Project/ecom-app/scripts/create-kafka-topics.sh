#!/bin/bash
# Create Kafka topics for HLD flow: orders, payments, deliveries.
# Run after docker-compose up (e.g. ./scripts/create-kafka-topics.sh).
# Topics may auto-create on first use; this script ensures they exist with explicit partition count.

KAFKA_CONTAINER="${KAFKA_CONTAINER:-kafka}"
BOOTSTRAP="${BOOTSTRAP:-localhost:9092}"
PARTITIONS="${PARTITIONS:-3}"
REPLICATION="${REPLICATION:-1}"

create_topic() {
  local topic=$1
  if docker exec "$KAFKA_CONTAINER" /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server "$BOOTSTRAP" \
    --create \
    --topic "$topic" \
    --partitions "$PARTITIONS" \
    --replication-factor "$REPLICATION" \
    --if-not-exists 2>/dev/null; then
    echo "Topic $topic created or already exists."
  else
    echo "Warning: Could not create topic $topic (container may not be ready or already exists)."
  fi
}

echo "Creating HLD topics (orders, payments, deliveries)..."
create_topic "orders"
create_topic "payments"
create_topic "deliveries"
echo "Done."
