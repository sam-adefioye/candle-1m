package org.com.controller;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.com.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@ApiResponses({
        @ApiResponse(responseCode = "200", content = { @Content(schema = @Schema(), mediaType = "application/json") }),
        @ApiResponse(responseCode = "404", content = { @Content(schema = @Schema(), mediaType = "application/json") }),
        @ApiResponse(responseCode = "500", content = { @Content(schema = @Schema(), mediaType = "application/json") })
})
@RequiredArgsConstructor
public class WebSocketController {

    private static final String MESSAGE = "Message";
    private static final String ERROR = "Error";
    @Autowired
    private final WebSocketService webSocketService;

    @PostMapping("/connect")
    public ResponseEntity<Map<String, String>> connect() {
        boolean success = webSocketService.connect();
        if (success) {
            return ResponseEntity.ok(Map.of(MESSAGE, "Connected to Kraken."));
        } else {
            return ResponseEntity.badRequest().body(Map.of(ERROR, "Error connecting to Kraken."));
        }
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, String>> subscribe() {
        if (!webSocketService.isConnected())
            return ResponseEntity.badRequest().body(Map.of(ERROR, "Not connected to Kraken, unable to subscribe. Please connect first."));

        webSocketService.subscribe();
        return ResponseEntity.ok(Map.of(MESSAGE, "Subscribing to channel: book."));
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<Map<String, String>> unsubscribe() {
        if (!webSocketService.isConnected())
            return ResponseEntity.badRequest().body(Map.of(ERROR, "Not connected to Kraken, unable to unsubscribe. Please connect and subscribe first."));

        webSocketService.unsubscribe();
        return ResponseEntity.ok(Map.of(MESSAGE, "Unsubscribed from channel: book."));
    }

    @PostMapping ("/disconnect")
    public ResponseEntity<Map<String, String>> disconnect() {
        if (!webSocketService.disconnect()) {
            return ResponseEntity.badRequest().body(Map.of(ERROR, "Error disconnecting, or already disconnected."));
        }
        return ResponseEntity.ok(Map.of(MESSAGE, "Disconnected from Kraken."));
    }
}
