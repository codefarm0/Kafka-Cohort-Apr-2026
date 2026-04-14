## topic creation

```
./bin/kafka-topics.sh --create --topic orders2 --partitions 1 --replication-factor 1 --config segment.bytes=1048576 --config segment.ms=36000 --bootstrap-server localhost:9092

```