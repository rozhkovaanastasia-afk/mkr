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

public class ConsumerApp {

    private static final String BOOTSTRAP_SERVERS = "kafka:29092";

    private static final String REQUEST_TOPIC = "demo-requests";
    private static final String RESPONSE_TOPIC = "demo-responses";

    public static void main(String[] args) {
        createTopics();

        KafkaConsumer<String, String> consumer = createConsumer();
        KafkaProducer<String, String> producer = createProducer();

        consumer.subscribe(Collections.singletonList(REQUEST_TOPIC));

        System.out.println("Чекаю запитів у '" + REQUEST_TOPIC + "'.");

        while (true) {
            ConsumerRecords<String, String> records =
                    consumer.poll(Duration.ofMillis(1000));

            for (var record : records) {
                String value = record.value();

                String[] parts = value.split(",");

                int start = Integer.parseInt(parts[0]);
                int finish = Integer.parseInt(parts[1]);

                System.out.println("<- Отримано запит: start=" + start + " finish=" + finish);

                double avgSteps = calculateAverageCollatzSteps(start, finish);

                Header header = record.headers().lastHeader("correlation-id");

                if (header == null) {
                    System.out.println("Запит без correlation-id пропущено.");
                    continue;
                }

                String correlationId = new String(header.value(), StandardCharsets.UTF_8);

                ProducerRecord<String, String> response =
                        new ProducerRecord<>(RESPONSE_TOPIC, String.valueOf(avgSteps));

                response.headers().add(
                        "correlation-id",
                        correlationId.getBytes(StandardCharsets.UTF_8)
                );

                producer.send(response);
                producer.flush();

                System.out.println("-> Надіслано відповідь: avgSteps=" + avgSteps);
            }
        }
    }

    private static long collatzSteps(long n) {
        long steps = 0;

        while (n != 1) {
            if (n % 2 == 0) {
                n = n / 2;
            } else {
                n = 3 * n + 1;
            }

            steps++;
        }

        return steps;
    }

    private static double calculateAverageCollatzSteps(int start, int finish) {
        long totalSteps = 0;
        int count = 0;

        for (int number = start; number <= finish; number++) {
            totalSteps += collatzSteps(number);
            count++;
        }

        return (double) totalSteps / count;
    }

    private static KafkaConsumer<String, String> createConsumer() {
        Properties props = new Properties();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "demo-responder-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        return new KafkaConsumer<>(props);
    }

    private static KafkaProducer<String, String> createProducer() {
        Properties props = new Properties();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        return new KafkaProducer<>(props);
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