
## Topic-level config 

```bash
kafka-topics.sh --create \
  --topic test-retention \
  --bootstrap-server localhost:9092 \
  --partitions 1 \
  --replication-factor 1 \
  --config retention.ms=60000 \
  --config retention.bytes=1048576
```

### Meaning:

* `retention.ms=60000` → messages kept for **1 minute**
* `retention.bytes=1048576` → max **1 MB per partition**

👉 Whichever limit is hit first, Kafka deletes old data.

---

##  Single Log File Size (Segment Rolling)

Kafka doesn’t store one infinite log file. It splits logs into **segments**.

You want:

* **small segment size**
* **time-based rolling also**

### Config:

```bash
--config segment.bytes=1048576 \
--config segment.ms=60000
```

### Meaning:

* `segment.bytes=1048576` → new log file after **1 MB**
* `segment.ms=60000` → new log file every **1 minute**

👉 Even if size isn’t reached, time will force a new segment.

This is key for:

* Faster cleanup
* Simulating high-churn systems

---

## Disable Auto Topic Creation

This is a **broker-level setting**, not topic-level.

### In `server.properties`:

```properties
auto.create.topics.enable=false
```

### Why this matters:

Without this, Kafka does this silently:

```java
producer.send("typo-topic", msg);
```

→ Kafka happily creates `typo-topic` 😅

With it disabled:

* Producer fails fast
* You enforce governance (very important in real systems)

---


##  Cleanup is not instant

    * Controlled by:

      ```properties
      log.retention.check.interval.ms
      ```

* Very small retention can:

    * break slow consumers
    * make debugging impossible

* Segment rolling impacts:

    * file handles
    * disk I/O patterns

## leader election

```properties
unclean.leader.election.enable = false
```