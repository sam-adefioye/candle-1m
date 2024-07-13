package org.com.service.impl;

import org.com.handler.WebSocketHandler;
import org.com.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class WebSocketServiceImplTest {

    @Mock
    private WebSocketHandler webSocketHandler;

    private WebSocketService underTest;

    @BeforeEach
    void setUp() {
        webSocketHandler = mock(WebSocketHandler.class);
        underTest = new WebSocketServiceImpl(webSocketHandler);
    }

    @Test
    void testConnectWhenAlreadyConnected() {
        doReturn(true).when(webSocketHandler).isConnected();
        assertTrue(underTest.connect());
    }

    @Test
    void testConnectWhenNotConnected() {
        doReturn(false).when(webSocketHandler).isConnected();
        doReturn(true).when(webSocketHandler).connect();
        assertTrue(underTest.connect());
    }

    @Test
    void testFailureToConnect() {
        doReturn(false).when(webSocketHandler).isConnected();
        doReturn(false).when(webSocketHandler).connect();
        assertFalse(underTest.connect());
    }

    @Test
    void testDisconnect() {
        doReturn(true).when(webSocketHandler).disconnect();
        assertTrue(webSocketHandler.disconnect());
    }

    @Test
    void testUnableToDisconnect() {
        doReturn(false).when(webSocketHandler).disconnect();
        assertFalse(webSocketHandler.disconnect());
    }

    @Test
    void testIsConnected() {
        doReturn(true).when(webSocketHandler).isConnected();
        assertTrue(webSocketHandler.isConnected());
    }

    @Test
    void testIsNotConnected() {
        doReturn(false).when(webSocketHandler).isConnected();
        assertFalse(webSocketHandler.isConnected());
    }
}