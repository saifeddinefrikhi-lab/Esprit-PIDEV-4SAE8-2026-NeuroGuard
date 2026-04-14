package com.neuroguard.consultationservice.service;

import com.neuroguard.consultationservice.dto.DistanceMatrixResultDto;
import com.neuroguard.consultationservice.dto.GeoCoordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Distance et durée par la route via Google Distance Matrix API.
 * Supporte plusieurs modes de transport (DRIVING, WALKING, TRANSIT, BICYCLING).
 * Résultats cachés dans Redis pour 2 heures.
 */
@Service
@ConditionalOnProperty(name = "app.distance-matrix.provider", havingValue = "google")
public class GoogleRoadDistanceService implements RoadDistanceService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleRoadDistanceService.class);
    private static final String MATRIX_URL = "https://maps.googleapis.com/maps/api/distancematrix/json";
    private static final String CACHE_PREFIX = "distance-matrix:";
    private static final long CACHE_TTL_MINUTES = 120;

    private final RestTemplate restTemplate;
    private final RedisTemplate<String, DistanceMatrixResultDto> redisTemplate;

    @Value("${app.distance-matrix.google.api-key:}")
    private String apiKey;

    public GoogleRoadDistanceService(RestTemplate restTemplate,
                                     RedisTemplate<String, DistanceMatrixResultDto> redisTemplate) {
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Calcule la distance et durée entre deux coordonnées avec un mode de transport spécifique.
     * Résultat mis en cache pour 2 heures.
     *
     * @param origin Point de départ
     * @param destination Point d'arrivée
     * @param transportMode Mode: DRIVING, WALKING, TRANSIT, BICYCLING
     * @return DistanceMatrixResultDto avec distance en mètres et durée en secondes
     */
    @Cacheable(value = "distance-matrix", key = "#origin.latitude + ',' + #origin.longitude + ',' + #destination.latitude + ',' + #destination.longitude + ',' + #transportMode")
    public DistanceMatrixResultDto getDistanceAndDuration(GeoCoordinates origin, GeoCoordinates destination, String transportMode) {
        if (!isAvailable()) {
            return null;
        }

        String cacheKey = buildCacheKey(origin, destination, transportMode);
        DistanceMatrixResultDto cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            logger.debug("Cache hit for distance matrix: origin={}, destination={}, mode={}", 
                origin, destination, transportMode);
            return cached;
        }

        DistanceMatrixResultDto result = callGoogleDistanceMatrix(origin, destination, transportMode);
        
        if (result != null) {
            redisTemplate.opsForValue().set(cacheKey, result, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            logger.debug("Cached distance matrix result: {} km, {} seconds", 
                (double)result.getDistanceMeters() / 1000, result.getDurationSeconds());
        }
        
        return result;
    }

    /**
     * Méthode surchargée pour compatibilité avec RoadDistanceService (utilise DRIVING par défaut).
     */
    @Override
    public DistanceMatrixResultDto getDistanceAndDuration(GeoCoordinates origin, GeoCoordinates destination) {
        return getDistanceAndDuration(origin, destination, "DRIVING");
    }

    /**
     * Appel à Google Distance Matrix API avec gestion d'erreurs.
     */
    @SuppressWarnings("unchecked")
    private DistanceMatrixResultDto callGoogleDistanceMatrix(GeoCoordinates origin, GeoCoordinates destination, String transportMode) {
        try {
            String origins = origin.getLatitude() + "," + origin.getLongitude();
            String destinations = destination.getLatitude() + "," + destination.getLongitude();

            String url = UriComponentsBuilder.fromHttpUrl(MATRIX_URL)
                    .queryParam("origins", origins)
                    .queryParam("destinations", destinations)
                    .queryParam("mode", normalizeTransportMode(transportMode))
                    .queryParam("key", apiKey)
                    .toUriString();

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                logger.warn("No response from Google Distance Matrix API");
                return null;
            }

            String status = (String) response.get("status");
            if (!"OK".equals(status)) {
                logger.warn("Google Distance Matrix status: {} - {}", status, 
                    response.get("error_message"));
                return null;
            }

            List<Map<String, Object>> rows = (List<Map<String, Object>>) response.get("rows");
            if (rows == null || rows.isEmpty()) {
                logger.warn("Empty rows in Distance Matrix response");
                return null;
            }

            List<Map<String, Object>> elements = (List<Map<String, Object>>) rows.get(0).get("elements");
            if (elements == null || elements.isEmpty()) {
                logger.warn("Empty elements in Distance Matrix response");
                return null;
            }

            Map<String, Object> element = elements.get(0);
            String elementStatus = (String) element.get("status");
            if (!"OK".equals(elementStatus)) {
                logger.warn("Google Distance Matrix element status: {}", elementStatus);
                return null;
            }

            Map<String, Object> distance = (Map<String, Object>) element.get("distance");
            Map<String, Object> duration = (Map<String, Object>) element.get("duration");
            if (distance == null || duration == null) {
                logger.warn("Missing distance or duration in element");
                return null;
            }

            int distMeters = ((Number) distance.get("value")).intValue();
            int durSeconds = ((Number) duration.get("value")).intValue();
            
            logger.info("Google Distance Matrix: {} meters, {} seconds (mode: {})", 
                distMeters, durSeconds, transportMode);
            
            return new DistanceMatrixResultDto(distMeters, durSeconds);
        } catch (Exception e) {
            logger.error("Erreur Distance Matrix Google (mode: {}): {}", transportMode, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Normalise le mode de transport selon l'API Google.
     */
    private String normalizeTransportMode(String mode) {
        if (mode == null || mode.isBlank()) {
            return "driving";
        }
        return mode.toLowerCase();
    }

    /**
     * Construit une clé de cache pour les résultats.
     */
    private String buildCacheKey(GeoCoordinates origin, GeoCoordinates destination, String transportMode) {
        return CACHE_PREFIX + origin.getLatitude() + "," + origin.getLongitude() + "," +
               destination.getLatitude() + "," + destination.getLongitude() + "," +
               (transportMode != null ? transportMode.toLowerCase() : "driving");
    }

    /**
     * Efface le cache (utile pour les tests ou maintenance).
     */
    public void clearCache() {
        try {
            redisTemplate.delete(redisTemplate.keys(CACHE_PREFIX + "*"));
            logger.info("Distance matrix cache cleared");
        } catch (Exception e) {
            logger.error("Error clearing distance matrix cache: {}", e.getMessage());
        }
    }
}

