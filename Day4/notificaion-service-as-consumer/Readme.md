Here are the exact Kafka CLI commands to see **consumer details**:

---

## ✅ 1. List All Consumer Groups

```bash
./bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list
```

---

## ✅ 2. Describe a Consumer Group (Lag, Offsets, Members)

```bash
./bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group <group-name>
```

Example:

```bash
./bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group orders-service
```

---

## ✅ 3. See Consumer Group Members (Which Consumers Are Active)

```bash
./bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group <group-name> --members
```

---

## ✅ 4. See Detailed Consumer Assignment

```bash
./bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group <group-name> --verbose
```

---

## ✅ 5. Check Lag for a Specific Topic

```bash
./bin/kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group <group-name> --topic orders
```

---

## ✅ 6. Read Messages Directly From Topic (Console Consumer)

```bash
./bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic orders --from-beginning
```

---

If you want, I can show how to monitor lag continuously or check offsets per partition.
