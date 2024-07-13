package org.com.handler.impl;

import org.com.model.Candle;
import org.com.model.Message;
import org.com.model.OrderBook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OrderBookHandlerImplTest {

    private OrderBookHandlerImpl underTest;

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private OrderBook orderBook;

    @BeforeEach
    void setUp() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("test_snapshot.json");
        Message message = jsonMapper.readValue(inputStream, Message.class);
        String json = jsonMapper.writeValueAsString(message.data().get(0));
        orderBook = jsonMapper.readValue(json, OrderBook.class);
        underTest = new OrderBookHandlerImpl("snapshot", 10);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testHandleCandleUpdateWithNewCandle() {
        underTest.handleCandleUpdate(orderBook, "snapshot");
        Candle candle = underTest.getCandle();
        assertAll("Grouped assertions on candle state",
                () -> assertTrue(candle.getTimestamp() > 0),
                () -> assertNotNull(candle.getSymbol()),
                () -> assertNotNull(candle.getTimestampAsInstant()),
                () -> assertTrue(candle.getOpen() > 0),
                () -> assertTrue(candle.getLow() > 0)
        );
    }

    @Test
    void testHandleCandleUpdateWithNewOrderBookSnapshot() {
        underTest.handleCandleUpdate(orderBook, "snapshot");
        Candle candle = underTest.getCandle();
        OrderBook masterOrderBook = underTest.getMasterOrderBook();
        assertAll("Grouped assertions on candle state",
                () -> assertTrue(candle.getTimestamp() > 0),
                () -> assertNotNull(candle.getSymbol()),
                () -> assertNotNull(candle.getTimestampAsInstant()),
                () -> assertEquals(0.5667, candle.getOpen()),
                () -> assertEquals(0.5667, candle.getLow()),
                () -> assertEquals(0.5666, masterOrderBook.getHighestBid()),
                () -> assertEquals(0.5668, masterOrderBook.getLowestAsk())
        );
    }

    @Test
    void testHandleCandleUpdateWithNewOrderBookUpdate() throws IOException {
        // Order book update
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("test_update_bids.json");
        Message message = jsonMapper.readValue(inputStream, Message.class);
        String json = jsonMapper.writeValueAsString(message.data().get(0));
        OrderBook orderBookUpdate = jsonMapper.readValue(json, OrderBook.class);

        underTest.handleCandleUpdate(orderBook, "snapshot");
        underTest.handleCandleUpdate(orderBookUpdate, "update");

        Candle candle = underTest.getCandle();
        OrderBook masterOrderBook = underTest.getMasterOrderBook();
        assertAll("Grouped assertions on candle state",
                () -> assertTrue(candle.getTimestamp() > 0),
                () -> assertNotNull(candle.getSymbol()),
                () -> assertNotNull(candle.getTimestampAsInstant()),
                () -> assertEquals(0.5667, candle.getOpen()),
                () -> assertEquals(0.5667, candle.getLow()),
                () -> assertEquals(0.5666, masterOrderBook.getHighestBid()),
                () -> assertEquals(0.5668, masterOrderBook.getLowestAsk())
        );
    }

    @Test
    void testHandleCandleUpdateWithNewFinishedCandle() throws IOException, InterruptedException {
        // Order book update
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("test_update_bids.json");
        Message message = jsonMapper.readValue(inputStream, Message.class);
        String json = jsonMapper.writeValueAsString(message.data().get(0));
        OrderBook orderBookUpdate = jsonMapper.readValue(json, OrderBook.class);

        underTest.handleCandleUpdate(orderBook, "snapshot");
        // Simulate 1 minute candle
        System.out.println("Waiting for 1 minute to elapse...");
        TimeUnit.MINUTES.sleep(1);
        underTest.handleCandleUpdate(orderBookUpdate, "update");

        Candle candle = underTest.getCandle();
        assertAll("Grouped assertions on candle state",
                () -> assertEquals(0, candle.getTimestamp()),
                () -> assertNull(candle.getSymbol()),
                () -> assertNull(candle.getTimestampAsInstant()),
                () -> assertEquals(0, candle.getOpen()),
                () -> assertEquals(0, candle.getLow())
        );
    }

    @Test
    void testHandleUnsubscribe() {
        underTest.handleCandleUpdate(orderBook, "snapshot");
        underTest.handleUnsubscribe();
        Candle candle = underTest.getCandle();
        OrderBook masterOrderBook = underTest.getMasterOrderBook();
        assertAll("Grouped assertions on candle state",
                () -> assertEquals(0, candle.getTimestamp()),
                () -> assertNull(candle.getSymbol()),
                () -> assertNull(candle.getTimestampAsInstant()),
                () -> assertEquals(0, candle.getOpen()),
                () -> assertEquals(0, candle.getLow()),
                () -> assertEquals(Double.MIN_VALUE, masterOrderBook.getHighestBid()),
                () -> assertEquals(Double.MAX_VALUE, masterOrderBook.getLowestAsk()),
                () -> assertTrue(masterOrderBook.getBidsMap().isEmpty()),
                () -> assertTrue(masterOrderBook.getAsksMap().isEmpty())
        );
    }

    @Test
    void testHandleOrderBookUpdate() {
        underTest.handleOrderBookUpdate(orderBook);
        Candle candle = underTest.getCandle();
        OrderBook masterOrderBook = underTest.getMasterOrderBook();
        assertAll("Grouped assertions on candle state",
                () -> assertEquals("BTC/USD", masterOrderBook.getSymbol()),
                () -> assertEquals(0.5666, masterOrderBook.getHighestBid()),
                () -> assertEquals(0.5668, masterOrderBook.getLowestAsk()),
                () -> assertEquals(10, masterOrderBook.getBidsMap().size()),
                () -> assertEquals(10, masterOrderBook.getAsksMap().size()),
                () -> assertEquals(20, candle.getTicks())
        );
    }
}