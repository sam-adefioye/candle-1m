package org.com.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.com.model.Candle;
import org.com.model.MidPrice;

/**
 * Custom Serdes for JSON serialization/deserialization
 */
public class Serdes {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Serde<MidPrice> MidPrice() {
        return new Serde<>() {

            @Override
            public Serializer<MidPrice> serializer() {
                return (topic, data) -> {
                    try {
                        return objectMapper.writeValueAsBytes(data);
                    } catch (Exception e) {
                        throw new RuntimeException("Error serializing MidPrice", e);
                    }
                };
            }

            @Override
            public Deserializer<MidPrice> deserializer() {
                return (topic, data) -> {
                    try {
                        return objectMapper.readValue(data, MidPrice.class);
                    } catch (Exception e) {
                        throw new RuntimeException("Error deserializing MidPrice", e);
                    }
                };
            }
        };
    }

    public static Serde<Candle> Candle() {
        return new Serde<>() {

            @Override
            public Serializer<Candle> serializer() {
                return (topic, data) -> {
                    try {
                        return objectMapper.writeValueAsBytes(data);
                    } catch (Exception e) {
                        throw new RuntimeException("Error serializing Candle", e);
                    }
                };
            }

            @Override
            public Deserializer<Candle> deserializer() {
                return (topic, data) -> {
                    try {
                        return objectMapper.readValue(data, Candle.class);
                    } catch (Exception e) {
                        throw new RuntimeException("Error deserializing Candle", e);
                    }
                };
            }
        };
    }
}
