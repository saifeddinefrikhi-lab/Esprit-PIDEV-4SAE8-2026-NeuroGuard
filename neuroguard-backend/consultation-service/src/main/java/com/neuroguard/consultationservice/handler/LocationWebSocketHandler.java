package com.neuroguard.consultationservice.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuroguard.consultationservice.dto.LocationUpdateDto;
import com.neuroguard.consultationservice.service.LocationStreamingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler WebSocket pour le streaming de positions GPS.
 * Gère les connexions WebSocket, reçoit les mises à jour de location,
 * et les rebroadcaste aux clients abonnés.
 * 
 * Connexion: WebSocket {baseUrl}/ws-location/provider/{providerId}
 * Message: { "latitude": 48.86, "longitude": 2.35, "timestamp": 123456 }
 */
@Component
public class LocationWebSocketHandler implements WebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(LocationWebSocketHandler.class);

    private final LocationStreamingService locationStreamingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Mappe session WebSocket -> (providerId, patientId)
    private final Map<String, SessionInfo> sessionMap = new ConcurrentHashMap<>();

    // Mappe providerId -> liste de WebSocket sessions des subscribers
    private final Map<Long, Set<WebSocketSession>> providerSubscriberSessions = new ConcurrentHashMap<>();

    private static class SessionInfo {
        Long providerId;
        Long patientId;
        String sessionId;

        SessionInfo(Long providerId, Long patientId, String sessionId) {
            this.providerId = providerId;
            this.patientId = patientId;
            this.sessionId = sessionId;
        }
    }

    public LocationWebSocketHandler(LocationStreamingService locationStreamingService) {
        this.locationStreamingService = locationStreamingService;
    }

    /**
     * Appelée quand une connexion WebSocket est établie.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        String uri = session.getUri().toString();
        
        // Extraire providerId de l'URI: /ws-location/provider/{providerId}
        Long providerId = extractProviderIdFromUri(uri);
        
        logger.info("WebSocket connection established. Session: {}, Provider: {}", sessionId, providerId);

        SessionInfo info = new SessionInfo(providerId, null, sessionId);
        sessionMap.put(sessionId, info);

        // Ajouter cette session à la liste des subscribers pour ce médecin
        if (providerId != null) {
            locationStreamingService.registerSubscriber(providerId, sessionId);
            providerSubscriberSessions.computeIfAbsent(providerId, k -> Collections.synchronizedSet(new HashSet<>()))
                    .add(session);

            // Envoyer confirmation de connexion
            sendConfirmation(session, providerId);
        }
    }

    /**
     * Appelée quand un message WebSocket est reçu.
     * Attendu: LocationUpdateDto avec position du médecin.
     */
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        try {
            String payload = (String) message.getPayload();
            SessionInfo info = sessionMap.get(session.getId());

            if (info == null) {
                logger.warn("Unknown session: {}", session.getId());
                return;
            }

            // Parser l'update de location
            LocationUpdateDto update = objectMapper.readValue(payload, LocationUpdateDto.class);
            
            // S'assurer que l'ID du médecin matche
            if (!update.getProviderId().equals(info.providerId)) {
                logger.warn("Provider ID mismatch. Expected: {}, Got: {}", 
                    info.providerId, update.getProviderId());
                return;
            }

            // Stocker la position
            boolean stored = locationStreamingService.updateProviderLocation(update);
            
            if (stored) {
                // Rebroadcaster à tous les autres subscribers (patients suivant ce médecin)
                broadcastLocationToSubscribers(update.getProviderId(), update);
            }

        } catch (Exception e) {
            logger.error("Error handling WebSocket message: {}", e.getMessage(), e);
            sendError(session, "Error processing location update: " + e.getMessage());
        }
    }

    /**
     * Appelée en cas d'erreur WebSocket.
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket transport error for session {}: {}", 
            session.getId(), exception.getMessage(), exception);
        cleanupSession(session);
    }

    /**
     * Appelée quand la connexion WebSocket est fermée.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        logger.info("WebSocket connection closed. Session: {}, Status: {}", 
            session.getId(), closeStatus);
        cleanupSession(session);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * Rebroadcaste une mise à jour de position à tous les subscribers.
     */
    private void broadcastLocationToSubscribers(Long providerId, LocationUpdateDto update) {
        Set<WebSocketSession> sessions = providerSubscriberSessions.get(providerId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String message;
        try {
            message = objectMapper.writeValueAsString(update);
        } catch (Exception e) {
            logger.error("Error serializing location update: {}", e.getMessage());
            return;
        }

        TextMessage textMessage = new TextMessage(message);
        List<WebSocketSession> closedSessions = new ArrayList<>();

        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(textMessage);
                } catch (IOException e) {
                    logger.warn("Error sending location to subscriber session {}: {}", 
                        session.getId(), e.getMessage());
                    closedSessions.add(session);
                }
            } else {
                closedSessions.add(session);
            }
        }

        // Nettoyer les sessions fermées
        closedSessions.forEach(s -> {
            sessions.remove(s);
            sessionMap.remove(s.getId());
        });
    }

    /**
     * Nettoie les infos de session lors de la fermeture.
     */
    private void cleanupSession(WebSocketSession session) {
        String sessionId = session.getId();
        SessionInfo info = sessionMap.remove(sessionId);

        if (info != null && info.providerId != null) {
            locationStreamingService.unregisterSubscriber(info.providerId, sessionId);
            
            Set<WebSocketSession> sessions = providerSubscriberSessions.get(info.providerId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    providerSubscriberSessions.remove(info.providerId);
                }
            }
        }
    }

    /**
     * Envoie une confirmation de connexion au client.
     */
    private void sendConfirmation(WebSocketSession session, Long providerId) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "CONNECTION_CONFIRMED");
            response.put("provider_id", providerId);
            response.put("timestamp", System.currentTimeMillis());
            response.put("message", "Connected successfully");

            String message = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            logger.error("Error sending confirmation: {}", e.getMessage());
        }
    }

    /**
     * Envoie un message d'erreur au client.
     */
    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "ERROR");
            response.put("message", errorMessage);
            response.put("timestamp", System.currentTimeMillis());

            String message = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            logger.error("Error sending error message: {}", e.getMessage());
        }
    }

    /**
     * Extrait providerId de l'URI WebSocket ou des parameter de requête.
     * Format: /ws/location?providerId={providerId}
     */
    private Long extractProviderIdFromUri(String uri) {
        try {
            // Chercher providerId dans l'URL query string
            if (uri.contains("providerId=")) {
                String[] parts = uri.split("providerId=");
                if (parts.length > 1) {
                    String value = parts[1].split("&")[0];
                    return Long.parseLong(value);
                }
            }
            
            // Fallback: essayer d'extraire du chemin (legacy support)
            String[] pathParts = uri.split("/");
            if (pathParts.length > 0) {
                String lastPart = pathParts[pathParts.length - 1];
                if (!lastPart.isEmpty() && !lastPart.contains("?")) {
                    return Long.parseLong(lastPart);
                }
            }
        } catch (Exception e) {
            logger.warn("Error extracting provider ID from URI: {}", uri);
        }
        return null;
    }
}
