package org.com.service;

import lombok.extern.slf4j.Slf4j;
import org.com.client.WebSocketClientImpl;
import org.com.kafka.CandleKafkaConsumer;
import org.com.kafka.MidPriceKafkaProducer;
import org.com.model.OrderBook;

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

    private final OrderBook orderBook = new OrderBook();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final MidPriceKafkaProducer midPriceKafkaProducer = new MidPriceKafkaProducer();
    private final CandleKafkaConsumer kafkaConsumer = new CandleKafkaConsumer();
    private final CandleStream streamsGenerator = new CandleStream();
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final WebSocketClientImpl webSocketClient = new WebSocketClientImpl(URI.create(KRAKEN_WS_URL), midPriceKafkaProducer, orderBook, isConnected);
    private final AtomicBoolean kafkaEnabled = new AtomicBoolean(false);

    public static void main(String[] args) {
        OrderBookCandleGenerator app = new OrderBookCandleGenerator();
        app.initializeKafka();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down application...");
            app.shutdown();
        }));

        app.start();
    }

    public void start() {
        try {
            connectToKraken();
            Thread.currentThread().join();

        } catch (Exception e) {
            log.error("Application error", e);
        }
    }

    private void connectToKraken() throws Exception {
        webSocketClient.connect();

        while (!isConnected.get()) {
            Thread.sleep(1000);
        }

        if (!isConnected.get()) {
            throw new RuntimeException("Failed to connect to Kraken WebSocket");
        }
    }

    public void shutdown() {
        if (webSocketClient.isOpen()) {
            webSocketClient.close();
        }
        midPriceKafkaProducer.close();
        kafkaConsumer.close();
        streamsGenerator.stop();

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
            kafkaEnabled.set(true);
            log.info("Kafka integration enabled");

            kafkaConsumer.startConsuming();
            streamsGenerator.start();

        } catch (Exception e) {
            log.warn("Kafka not available, running without Kafka integration: {}", e.getMessage());
            kafkaEnabled.set(false);
        }
    }
}
