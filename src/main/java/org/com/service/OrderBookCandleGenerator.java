package org.com.service;

import lombok.extern.slf4j.Slf4j;
import org.com.client.WebSocketClientImpl;
import org.com.kafka.CandleKafkaConsumer;
import org.com.kafka.CandleKafkaProducer;
import org.com.model.Candle;
import java.net.URI;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main application that connects to Kraken WebSocket API to receive order book data
 * and generates 1-minute candles from tick-level data.
 * Assumptions:
 * - Using BTC/USD pair for demonstration
 * - Candles are generated every minute on the minute boundary
 * - Mid price calculation: (highest_bid + lowest_ask) / 2
 * - Order book is maintained in memory with concurrent access support
 */
@Slf4j
public class OrderBookCandleGenerator {
    private static final String KRAKEN_WS_URL = "wss://ws.kraken.com/v2";
    private static final String SYMBOL = "BTC/USD";

    private final OrderBook orderBook = new OrderBook();
    private final CandleGenerator candleGenerator = new CandleGenerator();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final CandleKafkaProducer kafkaProducer = new CandleKafkaProducer();
    private final CandleKafkaConsumer kafkaConsumer = new CandleKafkaConsumer();
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final WebSocketClientImpl webSocketClient = new WebSocketClientImpl(URI.create(KRAKEN_WS_URL), candleGenerator, orderBook, isConnected);

    private boolean kafkaEnabled = false;

    public static void main(String[] args) {
        OrderBookCandleGenerator app = new OrderBookCandleGenerator();
        app.initializeKafka();

        // Add shutdown hook for graceful cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down application...");
            app.shutdown();
        }));

        app.start();
    }

    public void start() {
        try {
            connectToKraken();
            startCandleGeneration();

            // Keep the application running
            Thread.currentThread().join();

        } catch (Exception e) {
            log.error("Application error", e);
        }
    }

    private void connectToKraken() throws Exception {
        webSocketClient.connect();

        // Wait for connection
        while (!isConnected.get()) {
            Thread.sleep(1000);
        }

        if (!isConnected.get()) {
            throw new RuntimeException("Failed to connect to Kraken WebSocket");
        }
    }

    private void startCandleGeneration() {
        // Calculate delay to next minute boundary
        long now = System.currentTimeMillis();
        long nextMinute = ((now / 60000) + 1) * 60000;
        long initialDelay = nextMinute - now;

        // Schedule candle generation every minute
        scheduler.scheduleAtFixedRate(() -> {
            try {
                Candle candle = candleGenerator.generateCandle(SYMBOL);
                if (candle != null) {
                    publishCandle(candle);
                }
            } catch (Exception e) {
                log.error("Error generating candle", e);
            }
        }, initialDelay, 60000, TimeUnit.MILLISECONDS);

        log.info("Candle generation scheduled to start in {} ms", initialDelay);
    }

    private void publishCandle(Candle candle) {
        if (candle == null) {
            return;
        }

        // Log to console (original requirement)
        log.info("Generated candle: {}", candle);

        // Publish to Kafka if enabled (bonus requirement)
        if (kafkaEnabled) {
            try {
                kafkaProducer.publishCandle(candle);
            } catch (Exception e) {
                log.error("Failed to publish candle to Kafka", e);
            }
        }
    }

    public void shutdown() {
        if (webSocketClient.isOpen()) {
            webSocketClient.close();
        }
        kafkaProducer.close();
        kafkaConsumer.close();

        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void initializeKafka() {
        try {
            kafkaEnabled = true;

            log.info("Kafka integration enabled");

            // Start consuming in a separate thread
            kafkaConsumer.startConsuming();

        } catch (Exception e) {
            log.warn("Kafka not available, running without Kafka integration: {}", e.getMessage());
            kafkaEnabled = false;
        }
    }
}
