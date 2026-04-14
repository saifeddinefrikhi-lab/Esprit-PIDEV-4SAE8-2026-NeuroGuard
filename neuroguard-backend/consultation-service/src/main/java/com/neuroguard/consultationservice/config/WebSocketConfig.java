package com.neuroguard.consultationservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Configuration WebSocket pour streaming de position GPS en temps réel.
 * Endpoint: /ws/location?providerId={providerId}
 * 
 * Utilisation:
 * 1. Client (Médecin): WebSocket {baseUrl}/ws/location?providerId={providerId}
 *    Envoie: { "latitude": 48.86, "longitude": 2.35, "timestamp": 123456 }
 * 2. Server rebroadcaste à tous les clients connectés (patients)
 * 3. Positions cachées en Redis avec TTL 5 minutes
 */
@Configuration
@EnableWebSocket
@ConditionalOnProperty(name = "app.websocket.enabled", havingValue = "true", matchIfMissing = true)
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler locationWebSocketHandler;

    public WebSocketConfig(WebSocketHandler locationWebSocketHandler) {
        this.locationWebSocketHandler = locationWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(locationWebSocketHandler, "/ws/location")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}
