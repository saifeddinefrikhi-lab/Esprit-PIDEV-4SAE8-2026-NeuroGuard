package com.neuroguard.consultationservice.service;

import com.neuroguard.consultationservice.dto.LocationUpdateDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service pour gérer le streaming de positions GPS en temps réel.
 * Stocke les positions dans Redis avec TTL, gère les WebSocket connections.
 */
@Service
public class LocationStreamingService {

    private static final Logger logger = LoggerFactory.getLogger(LocationStreamingService.class);

    private static final String LOCATION_CACHE_PREFIX = "location:provider:";
    private static final String PROVIDER_SESSIONS_KEY = "provider:sessions";

    private final RedisTemplate<String, LocationUpdateDto> locationRedisTemplate;
    private final Map<Long, List<String>> providerSubscribers = new ConcurrentHashMap<>();  // providerId -> list of session IDs

    @Value("${app.location.streaming.cache-ttl-seconds:300}")
    private long cacheTtlSeconds;

    @Value("${app.location.streaming.broadcast-interval-ms:5000}")
    private long broadcastIntervalMs;

    public LocationStreamingService(RedisTemplate<String, LocationUpdateDto> locationRedisTemplate) {
        this.locationRedisTemplate = locationRedisTemplate;
    }

    /**
     * Enregistre une mise à jour de position d'un médecin.
     * Stocke en Redis et déclenche rebroadcast aux patients connectés.
     *
     * @param update Mise à jour de position
     * @return true si stockée avec succès
     */
    public boolean updateProviderLocation(LocationUpdateDto update) {
        if (update == null || update.getProviderId() == null || 
            update.getLatitude() == null || update.getLongitude() == null) {
            logger.warn("Invalid location update: missing required fields");
            return false;
        }

        try {
            // Assurer que timestamp existe
            if (update.getTimestamp() == null) {
                update.setTimestamp(System.currentTimeMillis());
            }

            String cacheKey = LOCATION_CACHE_PREFIX + update.getProviderId();
            locationRedisTemplate.opsForValue().set(cacheKey, update, cacheTtlSeconds, TimeUnit.SECONDS);

            logger.debug("Location updated for provider {}: {}, {}", 
                update.getProviderId(), update.getLatitude(), update.getLongitude());

            // Déclencher rebroadcast aux subscribers
            broadcastToSubscribers(update);

            return true;
        } catch (Exception e) {
            logger.error("Error updating provider location: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Récupère la position actuelle d'un médecin depuis Redis.
     *
     * @param providerId ID du médecin
     * @return Position actuelle ou null si pas trouvée/expirée
     */
    public LocationUpdateDto getProviderLocation(Long providerId) {
        try {
            String cacheKey = LOCATION_CACHE_PREFIX + providerId;
            LocationUpdateDto location = locationRedisTemplate.opsForValue().get(cacheKey);

            if (location != null) {
                logger.debug("Retrieved location for provider {}: age {} ms",
                    providerId, System.currentTimeMillis() - location.getTimestamp());
            }

            return location;
        } catch (Exception e) {
            logger.error("Error retrieving provider location: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Enregistre un session pour un subscriber (patient) qui suit un médecin.
     *
     * @param providerId ID du médecin suivi
     * @param sessionId ID unique de la session WebSocket
     */
    public void registerSubscriber(Long providerId, String sessionId) {
        providerSubscribers.computeIfAbsent(providerId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(sessionId);
        logger.info("Subscriber {} registered for provider {}", sessionId, providerId);
    }

    /**
     * Désenregistre un subscriber.
     *
     * @param providerId ID du médecin
     * @param sessionId ID de la session WebSocket
     */
    public void unregisterSubscriber(Long providerId, String sessionId) {
        List<String> sessions = providerSubscribers.get(providerId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                providerSubscribers.remove(providerId);
                logger.info("No more subscribers for provider {}, cleaned up", providerId);
            } else {
                logger.info("Subscriber {} unregistered for provider {}", sessionId, providerId);
            }
        }
    }

    /**
     * Récupère la liste des subscribers (sessions connectées) pour un médecin.
     *
     * @param providerId ID du médecin
     * @return Liste des IDs de session
     */
    public List<String> getSubscribers(Long providerId) {
        List<String> sessions = providerSubscribers.get(providerId);
        return sessions != null ? new ArrayList<>(sessions) : Collections.emptyList();
    }

    /**
     * Rebroadcaste une mise à jour de position aux subscribers.
     * Cette méthode peut être appelée par le WebSocket handler.
     *
     * @param update Mise à jour à rebroadcaster
     */
    private void broadcastToSubscribers(LocationUpdateDto update) {
        List<String> subscribers = getSubscribers(update.getProviderId());
        if (!subscribers.isEmpty()) {
            logger.debug("Broadcasting location update to {} subscribers of provider {}",
                subscribers.size(), update.getProviderId());

            // Le WebSocket handler fera le rebroadcast réel aux connexions WebSocket
            // Cette info est stockée pour que le handler puisse la récupérer
        }
    }

    /**
     * Efface la position cachée d'un médecin (ex: fin de tournée).
     *
     * @param providerId ID du médecin
     */
    public void clearProviderLocation(Long providerId) {
        try {
            String cacheKey = LOCATION_CACHE_PREFIX + providerId;
            locationRedisTemplate.delete(cacheKey);
            logger.info("Cleared location for provider {}", providerId);
        } catch (Exception e) {
            logger.error("Error clearing provider location: {}", e.getMessage());
        }
    }

    /**
     * Efface tous les caches de location (maintenance).
     */
    public void clearAllLocations() {
        try {
            Set<String> keys = locationRedisTemplate.keys(LOCATION_CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                locationRedisTemplate.delete(keys);
                logger.info("Cleared {} provider locations", keys.size());
            }
        } catch (Exception e) {
            logger.error("Error clearing all locations: {}", e.getMessage());
        }
    }

    /**
     * Récupère le nombre de médecins actuellement en tournée (avec position cachée).
     */
    public long getActiveProvidersCount() {
        try {
            Set<String> keys = locationRedisTemplate.keys(LOCATION_CACHE_PREFIX + "*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            logger.error("Error getting active providers count: {}", e.getMessage());
            return 0;
        }
    }
}
