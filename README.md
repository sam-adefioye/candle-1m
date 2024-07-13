# Usage:
Run application either via IDE or `java -jar target/candle-1m-0.0.1.jar`.

You can also run the application by using `./mvnw spring-boot:run or mvn spring-boot:run`. 

Alternatively, you can build the JAR file with `./mvnw clean package` or `mvn clean package` and then run the JAR file, using either of the above commands.

Then navigate to http://localhost:8080/swagger-ui/index.html.

# Assumptions:

* Only one exchange is used (Kraken)
* Orderbook data is collected and maintained for only one ticker/instrument (default: BTC/USD)
* The depth of the orderbook will be configurable (set to 10, configurable in `application.properties` file)
* Should be able to unsubscribe from websocket channel, and disconnect from exchange, and reconnect and resubscribe.