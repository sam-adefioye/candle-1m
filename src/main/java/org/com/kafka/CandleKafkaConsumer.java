package org.com.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.com.model.Candle;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Kafka consumer for consuming candle data
 */
public class CandleKafkaConsumer {
    private static final Logger logger = LoggerFactory.getLogger(CandleKafkaConsumer.class);
    private static final String TOPIC_NAME = "candle-data";
    private static final String CONSUMER_GROUP = "candle-consumer-group";

    private final Consumer<String, String> consumer;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;
    private volatile boolean running = false;

    public CandleKafkaConsumer() {
        this(getDefaultConsumerProperties());
    }

    public CandleKafkaConsumer(Properties properties) {
        this.consumer = new KafkaConsumer<>(properties);
        this.objectMapper = new ObjectMapper();
        this.executorService = Executors.newSingleThreadExecutor();

        logger.info("Kafka consumer initialized for topic: {} with group: {}", TOPIC_NAME, CONSUMER_GROUP);
    }

    private static Properties getDefaultConsumerProperties() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
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
            logger.warn("Consumer is already running");
            return;
        }

        running = true;
        consumer.subscribe(Collections.singletonList(TOPIC_NAME));

        executorService.submit(() -> {
            logger.info("Started consuming candles from Kafka topic: {}", TOPIC_NAME);

            try {
                while (running) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            processRecord(record);
                        } catch (Exception e) {
                            logger.error("Error processing record: {}", record.value(), e);
                        }
                    }
                }
            } catch (Exception e) {
                if (running) {
                    logger.error("Error in consumer loop", e);
                }
            } finally {
                consumer.close();
                logger.info("Kafka consumer closed");
            }
        });
    }

    private void processRecord(ConsumerRecord<String, String> record) {
        try {
            // Deserialize the candle from JSON
            Candle candle = objectMapper.readValue(record.value(), Candle.class);

            // Log the consumed candle
            logger.info("Consumed candle from Kafka: {}", candle);

            // Here you could add additional processing logic, such as:
            // - Storing to database
            // - Forwarding to other systems
            // - Aggregating data
            // - Real-time analytics

        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize candle from record: {}", record.value(), e);
        }
    }

    /**
     * Closes the producer and releases resources
     */
    public void close() {
        try {
            consumer.close(Duration.ofSeconds(5));
            logger.info("Kafka producer closed");
        } catch (Exception e) {
            logger.error("Error closing Kafka producer", e);
        }
    }
}
