package org.com.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.com.model.Candle;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Kafka consumer for consuming candle data
 */
@Slf4j
public class CandleKafkaConsumer {
    private static final String TOPIC_NAME = "candle-data";
    private static final String CONSUMER_GROUP = "candle-consumer-group";

    private final Consumer<String, String> consumer;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    private volatile boolean running = false;

    public CandleKafkaConsumer() {
        Properties properties = getDefaultConsumerProperties();
        this.consumer = new KafkaConsumer<>(properties);
        this.objectMapper = new ObjectMapper();
        this.executorService = Executors.newSingleThreadExecutor();

        log.info("Kafka consumer initialized for topic: {} with group: {}", TOPIC_NAME, CONSUMER_GROUP);
    }

    private Properties getDefaultConsumerProperties() {
        Properties props = new Properties();
        String server = System.getenv("KAFKA_BOOTSTRAP_SERVERS");
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, server == null ? "localhost:9092" : server);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, CONSUMER_GROUP);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
        return props;
    }

    /**
     * Starts consuming candle data from Kafka
     */
    public void startConsuming() {
        if (running) {
            log.warn("Consumer is already running");
            return;
        }

        running = true;
        consumer.subscribe(Collections.singletonList(TOPIC_NAME));

        executorService.submit(() -> {
            log.info("Started consuming candles from Kafka topic: {}", TOPIC_NAME);

            try {
                while (running) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            processRecord(record);
                        } catch (Exception e) {
                            log.error("Error processing record: {}", record.value(), e);
                        }
                    }
                }
            } catch (Exception e) {
                if (running) {
                    log.error("Error in consumer loop", e);
                }
            } finally {
                consumer.close();
                log.info("Kafka consumer closed");
            }
        });
    }

    private void processRecord(ConsumerRecord<String, String> record) {
        try {
            // Deserialize the candle from JSON
            Candle candle = objectMapper.readValue(record.value(), Candle.class);

            // Log the consumed candle
            log.info("Consumed candle from Kafka: {}", candle);

            // Here you could add additional processing logic, such as:
            // - Storing to database
            // - Forwarding to other systems
            // - Aggregating data
            // - Real-time analytics

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize candle from record: {}", record.value(), e);
        }
    }

    /**
     * Closes the producer and releases resources
     */
    public void close() {
        try {
            consumer.close(Duration.ofSeconds(5));
            log.info("Kafka producer closed");
        } catch (Exception e) {
            log.error("Error closing Kafka producer", e);
        }
    }
}
