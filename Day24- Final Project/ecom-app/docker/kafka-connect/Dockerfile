# Kafka Connect: Debezium MySQL (base image) + Elasticsearch sink (OSS connector JARs from Confluent Maven).
#
# Note: Elasticsearch itself is the official docker.elastic.co image in docker-compose.
# This image only adds the *Kafka Connect plugin* that copies Kafka topics into ES — not a custom ES server.
#
# We avoid Confluent Hub API URLs (often 401/403 or missing versions like 14.0.10); see fetch-pom.xml.

FROM maven:3.9.9-eclipse-temurin-17 AS es-connector
WORKDIR /build
COPY fetch-pom.xml pom.xml
RUN mvn -q -B dependency:copy-dependencies -DincludeScope=runtime \
      -DoutputDirectory=/out/kafka-connect-elasticsearch \
  && mvn -q -B dependency:copy \
      -Dartifact=io.confluent:kafka-connect-elasticsearch:14.0.3:jar \
      -DoutputDirectory=/out/kafka-connect-elasticsearch

FROM debezium/connect:2.7.3.Final

USER root
# Flat plugin dir: all JARs on one classpath (Kafka Connect plugin path layout)
COPY --from=es-connector /out/kafka-connect-elasticsearch/ \
  /kafka/connect/kafka-connect-elasticsearch/
# Debezium entrypoint may symlink scripting JARs into each plugin dir — must be writable by `kafka`
RUN chown -R kafka:kafka /kafka/connect/kafka-connect-elasticsearch

USER kafka
