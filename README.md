# Kafka Request-Reply Collatz 

## Мета роботи

Реалізувати міжконтейнерну взаємодію через Apache Kafka за шаблоном request-reply.

## Опис роботи

Проєкт складається з двох окремих Java-сервісів:

- Producer;
- Consumer.

Producer надсилає запит у Kafka-топік `demo-requests` у форматі:

```text
start,finish
```

Consumer отримує запит, обчислює середню кількість кроків послідовності Колатца для чисел із заданого діапазону та надсилає відповідь у топік `demo-responses`.

Для зв’язування запиту й відповіді використовується `correlation-id`.

---

# Створення Docker-мережі

```bash
docker network create kafka-net
```

---

# Запуск Kafka

```bash
docker run -d --name kafka --network kafka-net -p 9092:9092 -e KAFKA_NODE_ID=1 -e KAFKA_PROCESS_ROLES=broker,controller -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@kafka:9093 -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:29092,CONTROLLER://0.0.0.0:9093,PLAINTEXT_HOST://0.0.0.0:9092 -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092 -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT -e KAFKA_INTER_BROKER_LISTENER_NAME=PLAINTEXT -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 -e KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS=0 -e KAFKA_AUTO_CREATE_TOPICS_ENABLE=true -e CLUSTER_ID=MkU3OEVBNTcwNTJENDM2Qk -v kafka-data:/var/lib/kafka/data confluentinc/cp-kafka:7.7.1
```

---

# Збірка Docker-образів

## Consumer

```bash
docker build -t kafka-demo-consumer ./consumer
```

## Producer

```bash
docker build -t kafka-demo-producer ./producer
```

---

# Запуск контейнерів

## Consumer

```bash
docker run -d --name kafka-consumer --network kafka-net kafka-demo-consumer
```

## Producer

```bash
docker run -d --name kafka-producer --network kafka-net kafka-demo-producer
```

---

# Перевірка роботи

## Перевірка контейнерів

```bash
docker ps
```

## Логи Consumer

```bash
docker logs kafka-consumer
```

## Логи Producer

```bash
docker logs kafka-producer
```

- Maven
