package org.com.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.com.service.CandleGenerator;
import org.com.service.OrderBook;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class WebSocketClientImpl extends WebSocketClient {
    private static final String SYMBOL = "BTC/USD";
    private static final String BIDS = "bids";
    private static final String ASKS = "asks";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final CandleGenerator candleGenerator;
    private final OrderBook orderBook;
    private final AtomicBoolean isConnected;

    public WebSocketClientImpl(URI serverUri, CandleGenerator candleGenerator, OrderBook orderBook, AtomicBoolean isConnected) {
        super(serverUri);
        this.candleGenerator = candleGenerator;
        this.orderBook = orderBook;
        this.isConnected = isConnected;
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        log.info("Connected to Kraken WebSocket");
        isConnected.set(true);
        subscribeToOrderBook();
    }

    @Override
    public void onMessage(String message) {
        try {
            processMessage(message);
        } catch (Exception e) {
            log.error("Error processing message: {}", message, e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.warn("WebSocket connection closed: {} - {}", code, reason);
        isConnected.set(false);
    }

    @Override
    public void onError(Exception ex) {
        log.error("WebSocket error", ex);
        isConnected.set(false);
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

            send(subscriptionMessage);
            log.info("Subscribed to order book for {}", SYMBOL);

        } catch (Exception e) {
            log.error("Failed to subscribe to order book", e);
        }
    }

    private void processMessage(String message) throws Exception {
        String channel;
        JsonNode root = objectMapper.readTree(message);

        if (root.has("channel") && (channel = root.get("channel").asText()).equals("book")) {
            log.debug("Received event: {}", channel);

            JsonNode dataNode = root.get("data");
            String type = root.get("type").asText();
            String symbol = dataNode.get(0).get("symbol").asText();

            if (dataNode.isArray() && SYMBOL.equals(symbol) && type.equals("snapshot")) {
                buildOrderBook(dataNode.get(0));
            } else if (dataNode.isArray() && SYMBOL.equals(symbol) && type.equals("update")) {
                processOrderBookUpdate(dataNode.get(0));
            }
        }
    }

    private void buildOrderBook(JsonNode data) {
        if (!(data.has(BIDS) && data.has(ASKS)))
            throw new IllegalArgumentException("Invalid order book snapshot data: " + data);

        orderBook.processSnapshot(data);
        if (!orderBook.isValid()) {
            throw new RuntimeException("Order book validation failed - highest bid >= lowest ask");
        }
        calculateMidPrice();
    }

    private void processOrderBookUpdate(JsonNode data) {
        if ((data.has(BIDS) || data.has(ASKS))) {
            orderBook.processDelta(data);
        }
        calculateMidPrice();
    }

    private void calculateMidPrice() {
        double midPrice = orderBook.getMidPrice();
        if (midPrice > 0) {
            candleGenerator.recordTick(midPrice);
        }
    }
}
