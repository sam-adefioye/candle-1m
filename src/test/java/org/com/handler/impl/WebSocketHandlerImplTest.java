package org.com.handler.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.com.handler.OrderBookHandler;
import org.com.model.Message;
import org.com.model.OrderBook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketHandlerImplTest {

    @Mock
    private WebSocketSession webSocketSession;

    @Mock
    private OrderBookHandler orderBookHandler;

    @Mock
    private StandardWebSocketClient webSocketClient;

    private WebSocketHandlerImpl underTest;

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private String messageJson;
    private OrderBook orderBook;

    @BeforeEach
    void setUp() throws IOException {
        messageJson = new String(getClass().getClassLoader().getResourceAsStream("test_snapshot.json").readAllBytes());
        Message message = jsonMapper.readValue(messageJson, Message.class);
        String orderBookDataJson = jsonMapper.writeValueAsString(message.data().get(0));
        orderBook = jsonMapper.readValue(orderBookDataJson, OrderBook.class);

        webSocketSession = mock(WebSocketSession.class);
        orderBookHandler = mock(OrderBookHandler.class);
        webSocketClient = mock(StandardWebSocketClient.class);
        underTest = new WebSocketHandlerImpl(orderBookHandler, webSocketSession, "book",
                "wss://ws.kraken.com/v2", webSocketClient);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testSendAndConfirmSuccess() throws IOException {
        messageJson = new String(getClass().getClassLoader().getResourceAsStream("test_subscribe.json").readAllBytes());
        doNothing().when(webSocketSession).sendMessage(any());
        assertDoesNotThrow(() -> underTest.sendAndConfirm(messageJson));
    }

    @Test
    void testHandleTextMessageForOrderBookData() {
        underTest.handleTextMessage(webSocketSession, new TextMessage(messageJson));
        verify(orderBookHandler).handleCandleUpdate(orderBook, "snapshot");
    }

    @Test
    void testUnsubscribeHandleTextMessage() throws IOException {
        messageJson = new String(getClass().getClassLoader().getResourceAsStream("test_unsubscribe.json").readAllBytes());
        underTest.handleTextMessage(webSocketSession, new TextMessage(messageJson));
        verify(orderBookHandler).handleUnsubscribe();
    }

    @Test
    void testConnectSuccess() {
        doReturn(CompletableFuture.completedFuture(webSocketSession)).when(webSocketClient).execute(any(), any(), any(URI.class));
        doReturn(true).when(webSocketSession).isOpen();
        assertTrue(underTest.connect());
    }

    @Test
    void testConnectFailure() {
        doReturn(CompletableFuture.failedFuture(new Exception())).when(webSocketClient).execute(any(), any(), any(URI.class));
        doReturn(false).when(webSocketSession).isOpen();
        assertFalse(underTest.connect());
    }

    @Test
    void testDisconnectSuccess() {
        doReturn(false).when(webSocketSession).isOpen();
        assertTrue(underTest.disconnect());
    }

    @Test
    void testDisconnectFailure() {
        doReturn(true).when(webSocketSession).isOpen();
        assertFalse(underTest.disconnect());
    }

    @Test
    void testIsConnectedSuccess() {
        doReturn(true).when(webSocketSession).isOpen();
        assertTrue(underTest.isConnected());
    }

    @Test
    void testIsConnectedFailure() {
        webSocketSession = null;
        assertFalse(underTest.isConnected());
    }
}