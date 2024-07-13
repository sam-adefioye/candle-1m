package org.com.service;


public interface WebSocketService {
    boolean connect();

    boolean isConnected();

    void subscribe();

    void unsubscribe();

    boolean disconnect();
}
