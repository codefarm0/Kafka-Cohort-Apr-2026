
# When Kafka Producers Lose Events: Real Scenarios, Configs & Fixes

In most discussions around Kafka reliability, people focus heavily on brokers and consumers. But in real production systems, **events are often lost even before they safely reach Kafka** — right at the producer side.

If you’re building high-throughput systems (payments, orders, notifications), understanding these failure modes is critical.

Let’s break down **real scenarios where events are lost**, the **exact configs behind them**, and how to **fix them properly**.

---

## 1. Fire-and-Forget Producer (acks=0)

### What Happens

Producer sends a message and **does not wait for any acknowledgment**.

```properties
acks=0
```

* Message is sent over the network
* No guarantee broker received it
* If broker is down or network drops → **event is gone forever**

### Why It Happens

This is often used for **max throughput benchmarks** or mistakenly left in production.

### Fix

```properties
acks=all
```

**What this does:**

* Leader waits for all in-sync replicas (ISR)
* Guarantees durability (assuming ISR is configured correctly)

👉 Reference: Kafka Producer Configs
[https://kafka.apache.org/documentation/#producerconfigs](https://kafka.apache.org/documentation/#producerconfigs)

---

## 2. Leader Acknowledges but Data Still Lost (acks=1)

### What Happens

```properties
acks=1
```

Flow:

1. Leader receives message ✅
2. Sends ACK to producer ✅
3. Leader crashes BEFORE replication ❌

Result: **Message is lost**

### Root Cause

Replication is asynchronous.

### Fix

```properties
acks=all
min.insync.replicas=2
replication.factor=3
```

**Why this works:**

* Ensures multiple brokers have the data before ACK
* Prevents data loss on leader failure

👉 Reference: Kafka Replication
[https://kafka.apache.org/documentation/#replication](https://kafka.apache.org/documentation/#replication)

---

## 3. Retries Disabled (Transient Failures = Lost Events)

### What Happens

```properties
retries=0
```

* Temporary network glitch
* Broker overload
* Leader election happening

Producer fails → **event dropped**

### Fix

```properties
retries=Integer.MAX_VALUE
delivery.timeout.ms=120000
```

Also:

```properties
retry.backoff.ms=100
```

**Key Insight:**
Retries must be enabled for **any real production system**

---

## 4. Idempotency Disabled (Duplicates or Loss Under Retry)

### What Happens

Retries + failures can cause:

* Duplicate sends
* Out-of-order writes
* Or even drops in edge cases

### Fix

```properties
enable.idempotence=true
```

**What it guarantees:**

* Exactly-once per partition (no duplicates)
* Safe retries

**Auto-config changes when enabled:**

* `acks=all`
* `retries=Integer.MAX_VALUE`
* `max.in.flight.requests.per.connection <= 5`

👉 Reference: Idempotent Producer
[https://kafka.apache.org/documentation/#producerconfigs_enable.idempotence](https://kafka.apache.org/documentation/#producerconfigs_enable.idempotence)

---

## 5. max.in.flight.requests Misconfiguration (Reordering → Logical Loss)

### What Happens

```properties
max.in.flight.requests.per.connection=5
enable.idempotence=false
```

Scenario:

1. Batch A sent
2. Batch B sent
3. A fails, B succeeds
4. Retry A → arrives after B

Result:

* **Order broken**
* Downstream system may treat earlier event as invalid → **logical data loss**

### Fix

```properties
enable.idempotence=true
```

or strictly:

```properties
max.in.flight.requests.per.connection=1
```

---

## 6. Buffer Full → Record Dropped

### What Happens

```properties
buffer.memory=33554432
```

If producer cannot send fast enough:

* Buffer fills up
* New send() calls block → then timeout

```properties
max.block.ms=60000
```

After timeout:
→ **Producer throws exception → event lost (if not handled)**

### Fix

```properties
buffer.memory=67108864
max.block.ms=120000
```

AND (very important):
👉 Always handle exceptions in producer code

---

## 7. delivery.timeout.ms Expiry (Silent Drop)

### What Happens

```properties
delivery.timeout.ms=120000
```

If retries + batching + delays exceed this:
→ Kafka drops the record

This is **silent if not logged properly**

### Fix

* Increase timeout:

```properties
delivery.timeout.ms=300000
```

* Tune alongside:

```properties
linger.ms
request.timeout.ms
```

---

## 8. Application-Level Fire-and-Forget (Ignoring Future)

### What Happens (Java Example)

```java
producer.send(record);
```

* Async call
* No callback
* No `.get()`

If send fails → **you never know**

### Fix

```java
producer.send(record, (metadata, exception) -> {
    if (exception != null) {
        // retry or log
    }
});
```

OR

```java
producer.send(record).get();
```

---

## 9. Serialization Failures (Event Never Leaves Producer)

### What Happens

* Serializer throws exception
* Producer never sends the event

Common issues:

* Schema mismatch
* Null values
* Avro/JSON conversion failure

### Fix

* Proper error handling
* Schema validation before send
* Use Schema Registry (if applicable)

---

## 10. Misconfigured Batching (linger.ms + batch.size)

### What Happens

```properties
linger.ms=50
batch.size=16384
```

If app crashes before batch is sent:
→ Messages sitting in buffer are lost

### Fix

* Reduce linger for critical systems:

```properties
linger.ms=5
```

* Force flush on shutdown:

```java
producer.flush();
producer.close();
```

---

# Production-Safe Kafka Producer Config (Recommended Baseline)

```properties
acks=all
enable.idempotence=true
retries=Integer.MAX_VALUE
delivery.timeout.ms=300000
request.timeout.ms=30000
retry.backoff.ms=100
linger.ms=5
batch.size=32768
buffer.memory=67108864
max.in.flight.requests.per.connection=5
```

---

# Key Takeaways

* **Most data loss happens BEFORE Kafka guarantees anything**
* `acks=all + idempotence=true` is non-negotiable
* Retries must be enabled and tuned
* Application-level handling is as important as Kafka configs
* Timeouts (`delivery.timeout.ms`) can silently drop data

---

# Final Thought

Kafka is often called “durable”, but that durability **starts only after the broker safely writes the data**.

If your producer is misconfigured, **Kafka never even gets a chance to protect your data**.

---
