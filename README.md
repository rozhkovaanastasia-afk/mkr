# Kafka Request-Reply Collatz Java

## Мета

Реалізувати міжконтейнерну взаємодію через Apache Kafka за шаблоном request-reply.

## Опис роботи

Проєкт складається з двох окремих Java-сервісів:

- Producer;
- Consumer.

Producer надсилає запит у Kafka-топік `demo-requests` у форматі:

```text
start,finish