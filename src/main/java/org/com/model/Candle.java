package org.com.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.time.Instant;

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
    @JsonIgnore
    private StringBuilder stringBuilder = new StringBuilder();

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
