package org.com.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.com.model.Candle;
import org.com.model.MidPrice;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

/**
 * Kafka Streams application that processes mid-price data to generate 1-minute candles
 */
@Slf4j
public class CandleStream {

    private KafkaStreams streams;

    public void start() {
        StreamsBuilder builder = new StreamsBuilder();

        Properties props = getStreamsProperties();
        String inputTopic = props.getProperty("input.topic");
        String outputTopic = props.getProperty("output.topic");
        long windowDurationMs = Long.parseLong(props.getProperty("window.duration.ms"));

        KStream<String, MidPrice> input = builder.stream(inputTopic, Consumed.with(Serdes.String(), org.com.kafka.Serdes.MidPrice()));

        KTable<Windowed<String>, Candle> reduced = input
                .groupByKey(Grouped.valueSerde(org.com.kafka.Serdes.MidPrice()))
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMillis(windowDurationMs)))
                .aggregate(
                        () -> null,
                        (key, midPrice, accumulatorCandle) -> {
                            double price = midPrice.getMidPrice();
                            if (accumulatorCandle == null) {
                                return Candle.builder()
                                        .symbol(midPrice.getSymbol())
                                        .open(price)
                                        .high(price)
                                        .low(price)
                                        .close(price)
                                        .ticks(1)
                                        .build();
                            }
                            accumulatorCandle.setHigh(Math.max(accumulatorCandle.getHigh(), price));
                            accumulatorCandle.setLow(Math.min(accumulatorCandle.getLow(), price));
                            accumulatorCandle.setClose(price);
                            accumulatorCandle.setTicks(accumulatorCandle.getTicks() + 1);
                            return accumulatorCandle;
                        },
                        Materialized.with(Serdes.String(), org.com.kafka.Serdes.Candle())
                ).suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()));

        KStream<String, Candle> candles = reduced
                .toStream()
                .mapValues((windowed, candle) -> {
                    if (candle == null) return null;
                    candle.setTimestamp(windowed.window().start() / 1000); // to seconds
                    return candle;
                })
                .selectKey((windowed, candle) -> windowed.key())
                .filter((key, value) -> value != null);

        candles.to(outputTopic, Produced.valueSerde(org.com.kafka.Serdes.Candle()));

        streams = new KafkaStreams(builder.build(), props);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down Kafka Streams");
            if (streams != null) {
                streams.close();
            }
        }));

        streams.start();
        log.info("Kafka Streams started");
    }


    public void stop() {
        if (streams != null) {
            streams.close();
            log.info("Kafka Streams stopped");
        }
    }

    public static Properties getStreamsProperties() {
        Properties props = new Properties();
        try (InputStream input = CandleStream.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                props.load(input);
            }
        } catch (IOException e) {
            log.warn("Could not load application.properties", e);
        }

        String bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS");

        props.put("bootstrap.servers", bootstrapServers == null ? "localhost:9092" : bootstrapServers);
        props.put("application.id", props.getProperty("kafka.streams.application.id"));
        props.put("input.topic", props.getProperty("kafka.streams.input.topic"));
        props.put("output.topic", props.getProperty("kafka.streams.output.topic"));
        props.put("window.duration.ms", props.getProperty("kafka.streams.window.duration.ms"));
        props.put("grace.period.ms", props.getProperty("kafka.streams.grace.period.ms"));
        props.put("default.key.serde", org.apache.kafka.common.serialization.Serdes.String().getClass().getName());
        props.put("default.value.serde", org.apache.kafka.common.serialization.Serdes.String().getClass().getName());
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);

        return props;
    }
}
