package org.com.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CandleTest {

    private Candle candle;
    private static final String INSTRUMENT = "BTC/USD";

    @BeforeEach
    void setUp() {
        candle = new Candle();
        candle.setSymbol(INSTRUMENT);
        candle.setTimestamp(System.currentTimeMillis());
        candle.setTimestampAsInstant(Instant.ofEpochMilli(candle.getTimestamp()));
        candle.setOpen(1000.0);
        candle.setHigh(1000.0);
        candle.setLow(1000.0);
        candle.setClose(1000.0);
        candle.setTicks(1000);
        candle.getStringBuilder().setLength(0);
    }

    @Test
    void testSetTimestampAndInstant() {
        long currentTimestamp = System.currentTimeMillis();
        candle.setTimestampAndInstant(currentTimestamp);
        assertEquals(currentTimestamp, candle.getTimestamp());
        assertEquals(Instant.ofEpochMilli(currentTimestamp), candle.getTimestampAsInstant());
    }

    @Test
    void testClearResetsFieldValues() {
        candle.clear();
        assertAll("All Candle fields should be reset.",
                () -> assertNull(candle.getSymbol()),
                () -> assertNull(candle.getTimestampAsInstant()),
                () -> assertEquals(0, candle.getTimestamp()),
                () -> assertEquals(0, candle.getOpen()),
                () -> assertEquals(0, candle.getHigh()),
                () -> assertEquals(0, candle.getLow()),
                () -> assertEquals(0, candle.getClose()),
                () -> assertEquals(0, candle.getTicks()),
                () -> assertEquals(0, candle.getStringBuilder().length())
        );
    }

    @Test
    void testToString() {
        String candleStr = candle.toString();
        String expectedStr = String.format(
                "Candle: symbol=%s, timestamp=%s, open=%s, high=%s, low=%s, close=%s, ticks=%s",
                candle.getSymbol(), candle.getTimestamp(), candle.getOpen(), candle.getHigh(), candle.getLow(),
                candle.getClose(), candle.getTicks()
        );
        assertEquals(expectedStr, candleStr);
    }
}