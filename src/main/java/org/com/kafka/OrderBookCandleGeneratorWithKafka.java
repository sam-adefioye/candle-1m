package org.com.kafka;

import org.com.model.Candle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhanced main application with Kafka integration
 */
public class OrderBookCandleGeneratorWithKafka {
    private static final Logger logger = LoggerFactory.getLogger(OrderBookCandleGeneratorWithKafka.class);

    private CandleKafkaProducer kafkaProducer;
    private CandleKafkaConsumer kafkaConsumer;
    private boolean kafkaEnabled = false;

    public OrderBookCandleGeneratorWithKafka() {
        initializeKafka();
    }

    private void initializeKafka() {
        try {
            // Check if Kafka is available (simple check)
            kafkaProducer = new CandleKafkaProducer();
            kafkaConsumer = new CandleKafkaConsumer();
            kafkaEnabled = true;

            logger.info("Kafka integration enabled");

            // Start consuming in a separate thread
            kafkaConsumer.startConsuming();

        } catch (Exception e) {
            logger.warn("Kafka not available, running without Kafka integration: {}", e.getMessage());
            kafkaEnabled = false;
        }
    }

    /**
     * Enhanced candle generation method that publishes to Kafka
     */
    public void publishCandle(Candle candle) {
        if (candle == null) {
            return;
        }

        // Log to console (original requirement)
        logger.info("Generated candle: {}", candle);

        // Publish to Kafka if enabled (bonus requirement)
        if (kafkaEnabled && kafkaProducer != null) {
            try {
                kafkaProducer.publishCandle(candle);
                logger.debug("Candle published to Kafka");
            } catch (Exception e) {
                logger.error("Failed to publish candle to Kafka", e);
            }
        }
    }

    /**
     * Cleanup method
     */
    public void shutdown() {
        if (kafkaProducer != null) {
            kafkaProducer.close();
        }

        if (kafkaConsumer != null) {
            kafkaConsumer.close();
        }

        logger.info("Kafka components shut down");
    }

    public static void main(String[] args) {
        OrderBookCandleGeneratorWithKafka app = new OrderBookCandleGeneratorWithKafka();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(app::shutdown));

        // Demonstration: Create and publish some sample candles
        try {
            for (int i = 0; i < 5; i++) {
                Candle sampleCandle = createSampleCandle(i);
                app.publishCandle(sampleCandle);
                Thread.sleep(2000); // Wait 2 seconds between candles
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Keep application running to see consumed messages
        try {
            Thread.sleep(10000); // Run for 10 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        app.shutdown();
    }

    private static Candle createSampleCandle(int index) {
        long timestamp = System.currentTimeMillis() / 1000;
        double basePrice = 50000.0 + (index * 100);

        return Candle.builder().timestamp(timestamp)
                .open(basePrice)           // open
                .high(basePrice + 50)      // high
                .low(basePrice - 30)      // low
                .close(basePrice + 20)      // close
        .ticks(100 + index * 10).build();     // ticks

    }

//    /**
//     * Closes the producer and releases resources
//     */
//    public void close() {
//        kafkaProducer.close();
//    }
}