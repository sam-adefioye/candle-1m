package org.com.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Thread-safe order book implementation
 */

@Slf4j
public class OrderBook {

    // Using TreeMap for automatic sorting by price
    private final ConcurrentSkipListSet<Double> bids = new ConcurrentSkipListSet<>(Collections.reverseOrder()); // price -> quantity
    private final ConcurrentSkipListSet<Double> asks = new ConcurrentSkipListSet<>(); // price -> quantity

    public void processSnapshot(JsonNode data) {
        try {
            // Process bids
            if (data.has("bids")) {
                JsonNode bidsNode = data.get("bids");
                for (JsonNode bid : bidsNode) {
                    double price = bid.get("price").asDouble();
                    double quantity = bid.get("qty").asDouble();
                    if (quantity > 0) {
                        bids.add(price);
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
                        asks.add(price);
                    }
                }
            }

            log.debug("Order book snapshot updated - bids: {}, asks: {}", bids.size(), asks.size());

        } catch (Exception e) {
            log.error("Error updating order book snapshot", e);
        }
    }

    public void processDelta(JsonNode data) {
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
                        bids.add(price);
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
                        asks.add(price);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error updating order book delta", e);
        }

        if (bids.isEmpty() && asks.isEmpty())
            throw new RuntimeException("Order book is empty after processing delta");
    }

    public boolean isValid() {
        if (bids.isEmpty() || asks.isEmpty()) {
            return false;
        }

        double highestBid = bids.first();
        double lowestAsk = asks.first();

        return highestBid < lowestAsk;
    }

    public double getMidPrice() {
        if (bids.isEmpty() || asks.isEmpty()) {
            return 0.0;
        }

        double highestBid = bids.first();
        double lowestAsk = asks.first();

        return (highestBid + lowestAsk) / 2.0;
    }
}
