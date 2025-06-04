A terminal app printing 1-minute ticker candles for a set asset (also using Kafka).

# Usage:
Run application either via IDE or `java -jar target/candle-1m-0.0.1.jar`.

Alternatively, you can build the JAR file with `./mvnw clean package` or `mvn clean package` and then run the JAR file, using the above commands.

# Assumptions:

* Only one exchange is used (Kraken)
* Orderbook data is collected and maintained for only one ticker/instrument (default: BTC/USD)
