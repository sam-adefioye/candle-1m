package org.com.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.com.model.Candle;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import java.time.Duration;
import java.util.Properties;

/**
 * Kafka producer for publishing candle data
 */
@Slf4j
public class CandleKafkaProducer {
    private static final String TOPIC_NAME = "candle-data";

    private final Producer<String, String> producer;
    private final ObjectMapper objectMapper;

    public CandleKafkaProducer() {
        Properties properties = getDefaultProducerProperties();
        this.producer = new KafkaProducer<>(properties);
        this.objectMapper = new ObjectMapper();

        log.info("Kafka producer initialized for topic: {}", TOPIC_NAME);
    }

    private Properties getDefaultProducerProperties() {
        Properties props = new Properties();
        String server = System.getenv("KAFKA_BOOTSTRAP_SERVERS");
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, server == null ? "localhost:9092" : server);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        return props;
    }

    /**
     * Publishes a candle to Kafka
     * @param candle The candle to publish
     */
    public void publishCandle(Candle candle) {
        try {
            String key = String.valueOf(candle.getTimestamp());
            String value = objectMapper.writeValueAsString(candle);

            ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC_NAME, key, value);

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Failed to send candle to Kafka", exception);
                } else {
                    log.debug("Candle sent to Kafka - Topic: {}, Partition: {}, Offset: {}",
                            metadata.topic(), metadata.partition(), metadata.offset());
                }
            });

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize candle to JSON", e);
            throw new RuntimeException("Failed to serialize candle", e);
        }
    }

    /**
     * Closes the producer and releases resources
     */
    public void close() {
        try {
            producer.flush();
            producer.close(Duration.ofSeconds(5));
            log.info("Kafka producer closed");
        } catch (Exception e) {
            log.error("Error closing Kafka producer", e);
        }
    }
}
