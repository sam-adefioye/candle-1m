package org.com.handler.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.com.handler.OrderBookHandler;
import org.com.handler.WebSocketHandler;
import org.com.model.Message;
import org.com.model.OrderBook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.*;

@RequiredArgsConstructor
@AllArgsConstructor
@Component
public class WebSocketHandlerImpl extends TextWebSocketHandler implements WebSocketHandler {

    private static final String SUBSCRIBE = "subscribe";
    private static final String UNSUBSCRIBE = "unsubscribe";
    private static final TypeReference<List<OrderBook>> ORDER_BOOK_TYPE_REF = new TypeReference<>() {};
    private static final TypeReference<Message> PAYLOAD_TYPE_REF = new TypeReference<>() {};
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final Logger logger = LoggerFactory.getLogger(WebSocketHandlerImpl.class);
    private StandardWebSocketClient webSocketClient = new StandardWebSocketClient();
    private WebSocketSession clientSession;
    @Autowired
    private OrderBookHandler orderBookHandler;
    @Value("${kraken.url}")
    private String krakenUrl;
    @Value("${kraken.channel}")
    private String krakenChannel;

    public WebSocketHandlerImpl(OrderBookHandler orderBookHandler,
                                WebSocketSession clientSession,
                                String krakenChannel,
                                String krakenUrl,
                                StandardWebSocketClient webSocketClient) {
        this.orderBookHandler = orderBookHandler;
        this.clientSession = clientSession;
        this.krakenChannel = krakenChannel;
        this.krakenUrl = krakenUrl;
        this.webSocketClient = webSocketClient;
    }

    @Override
    public void sendAndConfirm(String msg) {
        try {
            clientSession.sendMessage(new TextMessage(msg));
            logger.info("Message successfully sent: {}", msg);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) {
        try {
            Message exchangeResponse = jsonMapper.readValue(message.getPayload(), PAYLOAD_TYPE_REF);
            String channel = exchangeResponse.channel();

            if (krakenChannel.equals(channel) && Objects.nonNull(exchangeResponse.data())) {
                String payloadData = jsonMapper.writeValueAsString(exchangeResponse.data());
                OrderBook orderBookData = jsonMapper.readValue(payloadData, ORDER_BOOK_TYPE_REF).get(0);
                String messageType = exchangeResponse.type();
                orderBookHandler.handleCandleUpdate(orderBookData, messageType);
            } else if (SUBSCRIBE.equals(exchangeResponse.method()) && exchangeResponse.success()) {
                logger.info("Subscribed to channel: book.");
            } else if (UNSUBSCRIBE.equals(exchangeResponse.method()) && exchangeResponse.success()) {
                logger.info("Unsubscribed from channel: book.");
                orderBookHandler.handleUnsubscribe();
            }
        } catch(JsonProcessingException e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public boolean connect() {
        try {
            clientSession = webSocketClient.execute(this, null, URI.create(krakenUrl)).get();
            logger.info("Successfully connected to Kraken via websockets.");
        } catch (Exception e) {
            logger.error("Exception while connecting to Kraken via websockets: ", e);
        }
        return isConnected();
    }

    @Override
    public boolean disconnect() {
        try {
            clientSession.close();
        } catch (IOException e) {
            logger.info(e.getMessage());
        }
        return !isConnected();
    }

    @Override
    public boolean isConnected() {
        return Objects.nonNull(clientSession) && clientSession.isOpen();
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        orderBookHandler.handleUnsubscribe();
        logger.info("Connection to Kraken closed.");
    }
}
