package org.com.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OrderBookTest {

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private OrderBook orderBook;

    @BeforeEach
    void setUp() throws IOException {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("test_snapshot.json");
        Message message = jsonMapper.readValue(inputStream, Message.class);
        String json = jsonMapper.writeValueAsString(message.data().get(0));
        orderBook = jsonMapper.readValue(json, OrderBook.class);
    }

    @Test
    void testClearResetsFieldValues() {
        orderBook.clear();
        assertAll("All orderBook fields should be reset.",
                () -> assertNull(orderBook.getSymbol()),
                () -> assertTrue(orderBook.getBids().isEmpty()),
                () -> assertTrue(orderBook.getAsks().isEmpty()),
                () -> assertEquals(0, orderBook.getChecksum()),
                () -> assertEquals(Double.MIN_VALUE, orderBook.getHighestBid()),
                () -> assertEquals(Double.MAX_VALUE, orderBook.getLowestAsk()),
                () -> assertTrue(orderBook.getAsksMap().isEmpty()),
                () -> assertTrue(orderBook.getBidsMap().isEmpty())
        );
    }
}