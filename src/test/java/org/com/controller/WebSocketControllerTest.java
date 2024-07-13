package org.com.controller;

import org.com.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketControllerTest {

    @Mock
    private WebSocketService webSocketService;

    private WebSocketController underTest;

    @BeforeEach
    void setUp() {
        webSocketService = mock(WebSocketService.class);
        underTest = new WebSocketController(webSocketService);
    }

    @Test
    void testConnectSuccess() {
        doReturn(true).when(webSocketService).connect();
        assertTrue(underTest.connect().getStatusCode().is2xxSuccessful());
    }
    @Test
    void testConnectFailure() {
        doReturn(false).when(webSocketService).connect();
        assertTrue(underTest.connect().getStatusCode().is4xxClientError());
    }

    @Test
    void testSubscribeWhenNotConnected() {
        doReturn(false).when(webSocketService).isConnected();
        assertTrue(underTest.subscribe().getStatusCode().is4xxClientError());
    }

    @Test
    void testSubscribeWhenConnected() {
        doReturn(true).when(webSocketService).isConnected();
        assertTrue(underTest.subscribe().getStatusCode().is2xxSuccessful());
    }

    @Test
    void testUnsubscribeWhenNotConnected() {
        doReturn(false).when(webSocketService).isConnected();
        assertTrue(underTest.unsubscribe().getStatusCode().is4xxClientError());
    }

    @Test
    void testUnsubscribeWhenConnected() {
        doReturn(true).when(webSocketService).isConnected();
        assertTrue(underTest.unsubscribe().getStatusCode().is2xxSuccessful());
    }

    @Test
    void testDisconnectWhenConnected() {
        doReturn(true).when(webSocketService).disconnect();
        assertTrue(underTest.disconnect().getStatusCode().is2xxSuccessful());
    }

    @Test
    void testDisconnectWhenNotConnected() {
        doReturn(false).when(webSocketService).disconnect();
        assertTrue(underTest.disconnect().getStatusCode().is4xxClientError());
    }
}