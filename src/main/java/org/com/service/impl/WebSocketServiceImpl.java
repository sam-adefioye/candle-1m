package org.com.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.com.handler.WebSocketHandler;
import org.com.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class WebSocketServiceImpl implements WebSocketService {

    @Value("${kraken.channel}")
    private String krakenChannel;
    @Value("${kraken.depth}")
    private int krakenOrderBookDepth;
    @Value("${kraken.instrument}")
    private String krakenInstrument;
    @Autowired
    private final WebSocketHandler webSocketHandler;

    @Override
    public boolean connect() {
        return webSocketHandler.isConnected() || webSocketHandler.connect();
    }

    @Override
    public void subscribe() {
        ObjectNode json = new ObjectMapper().createObjectNode();
        json.put("method", "subscribe");
        ObjectNode paramsJson = json.putObject("params");
        paramsJson.put("depth", krakenOrderBookDepth);
        paramsJson.put("channel", krakenChannel);
        ArrayNode symbolList = paramsJson.putArray("symbol");
        symbolList.add(krakenInstrument);
        webSocketHandler.sendAndConfirm(json.toString());
    }

    @Override
    public void unsubscribe() {
        ObjectNode json = new ObjectMapper().createObjectNode();
        json.put("method", "unsubscribe");
        ObjectNode paramsJson = json.putObject("params");
        paramsJson.put("depth", krakenOrderBookDepth);
        paramsJson.put("channel", krakenChannel);
        ArrayNode symbolList = paramsJson.putArray("symbol");
        symbolList.add(krakenInstrument);
        webSocketHandler.sendAndConfirm(json.toString());
    }

    @Override
    public boolean disconnect() {
        return webSocketHandler.disconnect();
    }

    @Override
    public boolean isConnected() {
        return webSocketHandler.isConnected();
    }
}
