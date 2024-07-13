package org.com.handler;


public interface WebSocketHandler {
    void sendAndConfirm(String msg);

    boolean connect();

    boolean isConnected();

    boolean disconnect();
}
