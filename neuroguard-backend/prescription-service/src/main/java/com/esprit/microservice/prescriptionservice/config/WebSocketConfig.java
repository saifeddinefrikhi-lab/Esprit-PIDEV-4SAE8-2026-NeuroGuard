package com.esprit.microservice.prescriptionservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple message broker for "/topic" destinations
        config.enableSimpleBroker("/topic", "/queue");
        // Set application destination prefix for @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/api/prescriptions/ws")
                .setAllowedOriginPatterns("http://localhost:4200", "http://127.0.0.1:4200")
                .withSockJS()
                .setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js");
        
        // Optional: Add raw WebSocket endpoint for clients that support it
        registry.addEndpoint("/api/prescriptions/ws-raw")
                .setAllowedOriginPatterns("http://localhost:4200", "http://127.0.0.1:4200");
    }
}
