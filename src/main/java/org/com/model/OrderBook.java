package org.com.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderBook {
    private String symbol;
    private List<Order> bids = new ArrayList<>();
    private List<Order> asks = new ArrayList<>();
    private long checksum;

    private Double highestBid = Double.MIN_VALUE;
    private Double lowestAsk = Double.MAX_VALUE;
    private final Map<Double, Order> asksMap = new HashMap<>();
    private final Map<Double, Order> bidsMap = new HashMap<>();

    public void clear() {
        symbol = null;
        bids.clear();
        asks.clear();
        checksum = 0;
        highestBid = Double.MIN_VALUE;
        lowestAsk = Double.MAX_VALUE;
        asksMap.clear();
        bidsMap.clear();
    }
}
