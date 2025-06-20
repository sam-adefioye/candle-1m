package org.com.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.com.model.Candle;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

/**
 * Main application that connects to Kraken WebSocket API to receive order book data
 * and generates 1-minute candles from tick-level data.
 * Assumptions:
 * - Using BTC/USD pair for demonstration
 * - Candles are generated every minute on the minute boundary
 * - Mid price calculation: (highest_bid + lowest_ask) / 2
 * - Order book is maintained in memory with concurrent access support
 */
public class OrderBookCandleGenerator {

    private static final Logger logger = LoggerFactory.getLogger(OrderBookCandleGenerator.class);
    private static final String KRAKEN_WS_URL = "wss://ws.kraken.com/v2";
    private static final String SYMBOL = "BTC/USD";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final OrderBook orderBook = new OrderBook();
    private final CandleGenerator candleGenerator = new CandleGenerator();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private WebSocketClient webSocketClient;
    private volatile boolean isConnected = false;

    public static void main(String[] args) {
        OrderBookCandleGenerator app = new OrderBookCandleGenerator();

        // Add shutdown hook for graceful cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down application...");
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
            logger.error("Application error", e);
        }
    }

    private void connectToKraken() throws Exception {
        URI serverUri = new URI(KRAKEN_WS_URL);

        webSocketClient = new WebSocketClient(serverUri) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                logger.info("Connected to Kraken WebSocket");
                isConnected = true;
                subscribeToOrderBook();
            }

            @Override
            public void onMessage(String message) {
                try {
                    processMessage(message);
                } catch (Exception e) {
                    logger.error("Error processing message: {}", message, e);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                logger.warn("WebSocket connection closed: {} - {}", code, reason);
                isConnected = false;
                // Implement reconnection logic here if needed
            }

            @Override
            public void onError(Exception ex) {
                logger.error("WebSocket error", ex);
                isConnected = false;
            }
        };

        webSocketClient.connect();

        // Wait for connection
        int attempts = 0;
        while (!isConnected && attempts < 10) {
            Thread.sleep(1000);
            attempts++;
        }

        if (!isConnected) {
            throw new RuntimeException("Failed to connect to Kraken WebSocket");
        }
    }

    private void subscribeToOrderBook() {
        try {
            // Subscribe to order book updates for BTC/USD
            String subscriptionMessage = """
                    {
                      "method": "subscribe",
                      "params": {
                          "channel": "book",
                          "depth": 10,
                          "symbol": [
                            "BTC/USD"
                          ]
                        }
                    }""";

            webSocketClient.send(subscriptionMessage);
            logger.info("Subscribed to order book for {}", SYMBOL);

        } catch (Exception e) {
            logger.error("Failed to subscribe to order book", e);
        }
    }

    private void processMessage(String message) throws Exception {
        JsonNode root = objectMapper.readTree(message);

        String channel;
        // Handle subscription status messages
        if (root.has("channel") && (channel = root.get("channel").asText()).equals("book")) {
            logger.debug("Received event: {}", channel);

            JsonNode dataNode = root.get("data");
            String type = root.get("type").asText();
            String symbol = dataNode.get(0).get("symbol").asText();
            // Handle order book data
            if (dataNode.isArray() && SYMBOL.equals(symbol)) {
                processOrderBookUpdate(dataNode.get(0), !type.equals("snapshot"));
            }
        }
    }

    private void processOrderBookUpdate(JsonNode data, boolean isUpdate) {
        try {
            // Handle snapshot (full order book)
            if (data.has("bids") && data.has("asks") && !isUpdate) {
                logger.debug("Processing order book snapshot");
                orderBook.updateSnapshot(data);
            }

            // Handle delta updates
            if ((data.has("bids") || data.has("asks")) && isUpdate) {
                logger.debug("Processing order book delta");
                orderBook.updateDelta(data);
            }

            // Validate order book after update
            if (!orderBook.isValid()) {
                logger.warn("Order book validation failed - highest bid >= lowest ask");
            }

            // Record tick for candle generation
            double midPrice = orderBook.getMidPrice();
            if (midPrice > 0) {
                candleGenerator.recordTick(midPrice);
            }

        } catch (Exception e) {
            logger.error("Error processing order book update", e);
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
                    logger.info("Generated candle: {}", candle);
                }
            } catch (Exception e) {
                logger.error("Error generating candle", e);
            }
        }, initialDelay, 60000, TimeUnit.MILLISECONDS);

        logger.info("Candle generation scheduled to start in {} ms", initialDelay);
    }

    public void shutdown() {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.close();
        }

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
}

/**
 * Represents an order book level with price and quantity
 */
class OrderBookLevel {
    private final double price;
    private final double quantity;

    public OrderBookLevel(double price, double quantity) {
        this.price = price;
        this.quantity = quantity;
    }

    public double getPrice() { return price; }
    public double getQuantity() { return quantity; }

    @Override
    public String toString() {
        return String.format("Level{price=%.5f, qty=%.8f}", price, quantity);
    }
}

/**
 * Thread-safe order book implementation
 */
class OrderBook {
    private static final Logger logger = LoggerFactory.getLogger(OrderBook.class);

    // Using TreeMap for automatic sorting by price
    private final Map<Double, Double> bids = new ConcurrentHashMap<>(); // price -> quantity
    private final Map<Double, Double> asks = new ConcurrentHashMap<>(); // price -> quantity

    private final Object updateLock = new Object();

    public void updateSnapshot(JsonNode data) {
        synchronized (updateLock) {
            try {
                // Clear existing data
//                bids.clear();
//                asks.clear();

                // Process bids
                if (data.has("bids")) {
                    JsonNode bidsNode = data.get("bids");
                    for (JsonNode bid : bidsNode) {
                        double price = bid.get("price").asDouble();
                        double quantity = bid.get("qty").asDouble();
                        if (quantity > 0) {
                            bids.put(price, quantity);
                        }
                    }
                }

                // Process asks
                if (data.has("asks")) {
                    JsonNode asksNode = data.get("asks");
                    for (JsonNode ask : asksNode) {
                        double price = ask.get("price").asDouble();
                        double quantity = ask.get("qty").asDouble();
                        if (quantity > 0) {
                            asks.put(price, quantity);
                        }
                    }
                }

                logger.debug("Order book snapshot updated - bids: {}, asks: {}", bids.size(), asks.size());

            } catch (Exception e) {
                logger.error("Error updating order book snapshot", e);
            }
        }
    }

    public void updateDelta(JsonNode data) {
        synchronized (updateLock) {
            try {
                // Process bid updates
                if (data.has("bids")) {
                    JsonNode bidsNode = data.get("bids");
                    for (JsonNode bid : bidsNode) {
                        double price = bid.get("price").asDouble();
                        double quantity = bid.get("qty").asDouble();

                        if (quantity == 0) {
                            bids.remove(price);
                        } else {
                            bids.put(price, quantity);
                        }
                    }
                }

                // Process ask updates
                if (data.has("asks")) {
                    JsonNode asksNode = data.get("asks");
                    for (JsonNode ask : asksNode) {
                        double price = ask.get("price").asDouble();
                        double quantity = ask.get("qty").asDouble();

                        if (quantity == 0) {
                            asks.remove(price);
                        } else {
                            asks.put(price, quantity);
                        }
                    }
                }

            } catch (Exception e) {
                logger.error("Error updating order book delta", e);
            }
        }
    }

    public boolean isValid() {
        if (bids.isEmpty() || asks.isEmpty()) {
            return false;
        }

        double highestBid = getHighestBid();
        double lowestAsk = getLowestAsk();

        return highestBid < lowestAsk;
    }

    public double getHighestBid() {
        return bids.keySet().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }

    public double getLowestAsk() {
        return asks.keySet().stream().mapToDouble(Double::doubleValue).min().orElse(Double.MAX_VALUE);
    }

    public double getMidPrice() {
        if (bids.isEmpty() || asks.isEmpty()) {
            return 0.0;
        }

        double highestBid = getHighestBid();
        double lowestAsk = getLowestAsk();

        return (highestBid + lowestAsk) / 2.0;
    }

    public int getBidCount() {
        return bids.size();
    }

    public int getAskCount() {
        return asks.size();
    }
}

/**
 * Generates 1-minute candles from tick data
 */
class CandleGenerator {
    private static final Logger logger = LoggerFactory.getLogger(CandleGenerator.class);

    private final List<Double> currentMinuteTicks = Collections.synchronizedList(new ArrayList<>());
    private final Object candleLock = new Object();

    public void recordTick(double midPrice) {
        synchronized (candleLock) {
            currentMinuteTicks.add(midPrice);
        }
    }

    public Candle generateCandle(final String symbol) {
        synchronized (candleLock) {
            if (currentMinuteTicks.isEmpty()) {
                logger.debug("No ticks recorded for current minute");
                return null;
            }

            // Calculate candle data
            long timestamp = Instant.now().truncatedTo(ChronoUnit.MINUTES).minusSeconds(60).getEpochSecond();
            double open = currentMinuteTicks.get(0);
            double close = currentMinuteTicks.get(currentMinuteTicks.size() - 1);
            double high = currentMinuteTicks.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            double low = currentMinuteTicks.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
            int ticks = currentMinuteTicks.size();

            // Clear ticks for next minute
            currentMinuteTicks.clear();

            return Candle.builder()
                    .symbol(symbol)
                    .timestamp(timestamp)
                    .open(open)
                    .high(high)
                    .low(low)
                    .close(close)
                    .ticks(ticks)
                    .build();
        }
    }
}

