package org.com.handler.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.com.handler.OrderBookHandler;
import org.com.model.Candle;
import org.com.model.Order;
import org.com.model.OrderBook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.*;

@Getter
@RequiredArgsConstructor
@Component
public class OrderBookHandlerImpl implements OrderBookHandler {
    
    private final Logger logger = LoggerFactory.getLogger(OrderBookHandlerImpl.class);
    private final OrderBook masterOrderBook = new OrderBook();
    private final List<Double> upToDepth = new ArrayList<>();
    private final Candle candle = new Candle();
    @Value("${kraken.type.snapshot}")
    private String krakenMessageType;
    @Value("${kraken.depth}")
    private int krakenOrderBookDepth;

    public OrderBookHandlerImpl(String krakenMessageType, int krakenOrderBookDepth) {

        this.krakenMessageType = krakenMessageType;
        this.krakenOrderBookDepth = krakenOrderBookDepth;
    }

    @Override
    public void handleCandleUpdate(@NonNull OrderBook orderBook, @NonNull String messageType) {
        handleOrderBookUpdate(orderBook);
        if (masterOrderBook.getBidsMap().isEmpty() || masterOrderBook.getAsksMap().isEmpty())
            logger.info("Missing bids and/or asks data!");

        double highestBid = masterOrderBook.getHighestBid();
        double lowestAsk = masterOrderBook.getLowestAsk();
        double midPrice = (highestBid + lowestAsk) / 2;

        if (candle.getTimestamp() == 0) {
            candle.setSymbol(masterOrderBook.getSymbol());
            candle.setTimestampAndInstant(System.currentTimeMillis());
            candle.setOpen(midPrice);
            candle.setLow(midPrice);
        }

        if (messageType.equals(krakenMessageType)) {
            if (!(masterOrderBook.getHighestBid() < masterOrderBook.getLowestAsk()))
                logger.info("Highest bid {} is not less than lowest ask {}", masterOrderBook.getHighestBid(),
                        masterOrderBook.getLowestAsk());

            candle.setHigh(midPrice);
            candle.setLow(midPrice);
        }
        else {
            if (midPrice > candle.getHigh())
                candle.setHigh(midPrice);
            if (midPrice < candle.getLow())
                candle.setLow(midPrice);
        }

        long minutesElapsed = candle.timestampToMinutesElapsed();
        if (minutesElapsed > 0) {
            candle.setClose(midPrice);
            logger.info(candle.toString());
            candle.clear();
        }
    }

    @Override
    public void handleOrderBookUpdate(@NonNull OrderBook orderBook) {
        if (!orderBook.getBids().isEmpty()) {
            updateOrderMap(orderBook.getBids(), masterOrderBook.getBidsMap(), true);
        }

        if (!orderBook.getAsks().isEmpty()) {
            updateOrderMap(orderBook.getAsks(), masterOrderBook.getAsksMap(), false);
        }
        candle.setTicks(candle.getTicks() + orderBook.getAsks().size() + orderBook.getBids().size());

        if (Objects.isNull(masterOrderBook.getSymbol()))
            masterOrderBook.setSymbol(orderBook.getSymbol());
    }

    @Override
    public void handleUnsubscribe() {
        upToDepth.clear();
        candle.clear();
        masterOrderBook.clear();
    }

    private void updateOrderMap(@NonNull List<Order> orders,
                                @NonNull Map<Double, Order> orderMap,
                                boolean isBid) {
        for (Order order: orders) {
            if (orderMap.containsKey(order.price()) && order.quantity().doubleValue() == 0) {
                orderMap.remove(order.price());
            }
            else {
                orderMap.put(order.price(), order);
            }

            if (isBid && order.price() > masterOrderBook.getHighestBid()) {
                masterOrderBook.setHighestBid(order.price());
            } else if (!isBid && order.price() < masterOrderBook.getLowestAsk()) {
                masterOrderBook.setLowestAsk(order.price());
            }
        }
        upToDepth.clear();

        if (orderMap.size() > krakenOrderBookDepth) {
            if (isBid) {
                upToDepth.addAll(orderMap.keySet().stream().sorted(Comparator.reverseOrder()).toList().subList(0, krakenOrderBookDepth));
            }
            else {
                upToDepth.addAll(orderMap.keySet().stream().sorted(Comparator.naturalOrder()).toList().subList(0, krakenOrderBookDepth));
            }
            orderMap.entrySet().removeIf(entry -> !upToDepth.contains(entry.getKey()));
        }
    }
}
