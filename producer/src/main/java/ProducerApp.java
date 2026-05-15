import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class ProducerApp {

    private static final String BOOTSTRAP_SERVERS = "kafka:29092";

    private static final String REQUEST_TOPIC = "demo-requests";
    private static final String RESPONSE_TOPIC = "demo-responses";

    public static void main(String[] args) throws Exception {
        createTopics();

        String correlationId = UUID.randomUUID().toString();

        int start = 10;
        int finish = 100;

        KafkaConsumer<String, String> responseConsumer = createConsumer();
        responseConsumer.subscribe(Collections.singletonList(RESPONSE_TOPIC));

        KafkaProducer<String, String> producer = createProducer();

        String requestValue = start + "," + finish;

        ProducerRecord<String, String> request =
                new ProducerRecord<>(REQUEST_TOPIC, requestValue);

        request.headers().add(
                "correlation-id",
                correlationId.getBytes(StandardCharsets.UTF_8)
        );

        producer.send(request).get();

        System.out.println("-> Запит надіслано: start=" + start + " finish=" + finish +
                " (id=" + correlationId + ")");

        long timeoutMillis = 3000_000;
        long startTime = System.currentTimeMillis();

        boolean received = false;

        while (!received && System.currentTimeMillis() - startTime < timeoutMillis) {
            ConsumerRecords<String, String> records =
                    responseConsumer.poll(Duration.ofMillis(1000));

            for (var record : records) {
                Header header = record.headers().lastHeader("correlation-id");

                if (header == null) {
                    continue;
                }

                String responseCorrelationId =
                        new String(header.value(), StandardCharsets.UTF_8);

                if (correlationId.equals(responseCorrelationId)) {
                    System.out.println("<- Отримано відповідь: avgSteps=" + record.value());
                    received = true;
                    break;
                }
            }
        }

        if (!received) {
            System.out.println("Відповідь не отримано за час очікування.");
        }

        System.out.println("Готово. Контейнер живе.");

        while (true) {
            Thread.sleep(60_000);
        }
    }

    private static KafkaProducer<String, String> createProducer() {
        Properties props = new Properties();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        return new KafkaProducer<>(props);
    }

    private static KafkaConsumer<String, String> createConsumer() {
        Properties props = new Properties();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "producer-response-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        return new KafkaConsumer<>(props);
    }

    private static void createTopics() {
        Properties props = new Properties();
        props.put("bootstrap.servers", BOOTSTRAP_SERVERS);

        try (AdminClient adminClient = AdminClient.create(props)) {
            NewTopic requestTopic = new NewTopic(REQUEST_TOPIC, 1, (short) 1);
            NewTopic responseTopic = new NewTopic(RESPONSE_TOPIC, 1, (short) 1);

            adminClient.createTopics(List.of(requestTopic, responseTopic));
        } catch (Exception ignored) {
        }
    }
}