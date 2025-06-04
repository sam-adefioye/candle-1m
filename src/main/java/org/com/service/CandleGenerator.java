package org.com.service;

import lombok.extern.slf4j.Slf4j;
import org.com.model.Candle;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates 1-minute candles from tick data
 */

@Slf4j
public class CandleGenerator {

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
                log.debug("No ticks recorded for current minute");
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
