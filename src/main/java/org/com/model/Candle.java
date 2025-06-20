package org.com.model;

import lombok.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Candle {
    private String symbol;
    private Instant timestampAsInstant;
    private long timestamp;
    private double open;
    private double high;
    private double low;
    private double close;
    private int ticks;
    @Builder.Default
    private StringBuilder stringBuilder = new StringBuilder();

    public long timestampToMinutesElapsed() {
        return timestampAsInstant.until(Instant.now(), ChronoUnit.MINUTES);
    }

    public void setTimestampAndInstant(long timestampAsMilli) {
        timestamp = timestampAsMilli;
        timestampAsInstant = Instant.ofEpochMilli(timestamp);
    }

    public void clear() {
        symbol = null;
        timestampAsInstant = null;
        timestamp = 0;
        open = 0;
        high = 0;
        low = 0;
        close = 0;
        ticks = 0;
        stringBuilder.setLength(0);
    }

    public String toString() {
        stringBuilder.setLength(0);
        stringBuilder.append("Candle: ");
        stringBuilder.append("symbol=").append(symbol);
        stringBuilder.append(", timestamp=").append(timestamp);
        stringBuilder.append(", open=").append(open);
        stringBuilder.append(", high=").append(high);
        stringBuilder.append(", low=").append(low);
        stringBuilder.append(", close=").append(close);
        stringBuilder.append(", ticks=").append(ticks);
        return stringBuilder.toString();
    }
}
