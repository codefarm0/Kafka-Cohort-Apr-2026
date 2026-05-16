# Custom Kafka Connect image (`ecom-kafka-connect:local`)

## What this is (and isn’t)

| Piece | Image / source |
|--------|----------------|
| **Elasticsearch** | Official `docker.elastic.co/elasticsearch/elasticsearch` in `docker-compose.yml` — unchanged, open distribution. |
| **This Dockerfile** | **Apache Kafka Connect** worker based on **Debezium Connect** + **Confluent Elasticsearch sink** plugin JARs. |

The Elasticsearch **sink** is a Connect plugin: it reads Kafka topics and writes documents into your existing ES cluster. It is **not** a replacement for the Elasticsearch container.

## Why not Confluent Hub `curl` in Docker?

The Hub API archive URL (`api.hub.confluent.io/.../versions/14.0.10/archive`) often fails in CI/Docker (HTTP 4xx, or versions that don’t exist on the CDN). **14.0.10** is not published on `packages.confluent.io/maven` (latest 14.x there is **14.0.3**).

This build uses **Maven** against Confluent’s **public Maven repository** (`https://packages.confluent.io/maven/`) to resolve:

- `io.confluent:kafka-connect-elasticsearch:14.0.3`
- plus all **runtime** transitive dependencies into one plugin directory.

## Connector license

The Confluent Elasticsearch sink is under the **Confluent Community License** (not Apache 2.0). It is widely used for dev/training; review Confluent’s terms for production use.

## Rebuild

From repo root:

```bash
docker compose build kafka-connect --no-cache
```

## Troubleshooting

### `ln: ... Permission denied` under `/kafka/connect/kafka-connect-elasticsearch/`

The Connect image runs as user **`kafka`**. Plugin files copied in the Dockerfile must be **`chown kafka:kafka`** so the Debezium entrypoint can create symlinks (e.g. `debezium-scripting-*.jar`) in each plugin directory. The Dockerfile includes that `chown`; rebuild the image if you still see this error.

### `ln: ... Read-only file system` under `/kafka/connect/scripts/`

Do **not** mount your repo’s `./scripts` folder onto `/kafka/connect/scripts` in Compose. That path is used by the Debezium image for **scripting JAR symlinks** when `ENABLE_DEBEZIUM_SCRIPTING=true`. A read-only bind mount blocks `ln`. Mount SQL/deploy helpers elsewhere (e.g. `/opt/ecom/scripts`) if you really need them inside the container — connector deployment is normally run **from the host** with `curl` to `http://localhost:8083`.

### MySQL `DECIMAL` looks like `"C7c="` in Kafka / Elasticsearch JSON

With Debezium’s default **`decimal.handling.mode=precise`**, `DECIMAL`/`NUMERIC` columns use Kafka Connect’s **Decimal** logical type. **`JsonConverter`** often serializes that as a **Base64 string** (not a JSON number) so precision is preserved — it is **not** random text.

This repo’s **`deploy-debezium-product-connector.sh`** sets **`decimal.handling.mode=string`** so values appear as normal decimal strings (e.g. `"29.99"`) in topics and in ES. **Redeploy** the Debezium connector after changing this; existing indexed docs may still show old encoding until you re-snapshot or reindex.
