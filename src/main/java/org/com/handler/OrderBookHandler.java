package org.com.handler;

import org.com.model.OrderBook;
import org.springframework.lang.NonNull;

public interface OrderBookHandler {
    void handleCandleUpdate(@NonNull OrderBook orderBook, String messageType);

    void handleOrderBookUpdate(@NonNull OrderBook orderBook);

    void handleUnsubscribe();
}
