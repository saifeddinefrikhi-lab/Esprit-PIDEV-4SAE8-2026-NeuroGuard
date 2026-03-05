package com.neuroguard.consultationservice.service;

import com.neuroguard.consultationservice.dto.GeoCoordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * Géocodage via Google Maps Geocoding API (nécessite une clé API).
 */
@Service
@ConditionalOnProperty(name = "app.geocoding.provider", havingValue = "google")
public class GoogleGeocodingService implements GeocodingService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleGeocodingService.class);
    private static final String GEOCODE_URL = "https://maps.googleapis.com/maps/api/geocode/json";

    private final RestTemplate restTemplate;

    @Value("${app.geocoding.google.api-key:}")
    private String apiKey;

    public GoogleGeocodingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public GeoCoordinates geocode(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }
        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("Google Geocoding : clé API non configurée (app.geocoding.google.api-key)");
            return null;
        }
        try {
            String url = UriComponentsBuilder.fromHttpUrl(GEOCODE_URL)
                    .queryParam("address", address)
                    .queryParam("key", apiKey)
                    .toUriString();

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                return null;
            }

            String status = (String) response.get("status");
            if (!"OK".equals(status)) {
                logger.warn("Google Geocoding status pour '{}': {}", address, status);
                return null;
            }

            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
            if (results == null || results.isEmpty()) {
                return null;
            }

            Map<String, Object> location = (Map<String, Object>) results.get(0).get("geometry");
            if (location == null) {
                return null;
            }
            Map<String, Object> latLng = (Map<String, Object>) location.get("location");
            if (latLng == null) {
                return null;
            }

            double lat = ((Number) latLng.get("lat")).doubleValue();
            double lon = ((Number) latLng.get("lng")).doubleValue();
            return new GeoCoordinates(lat, lon);
        } catch (Exception e) {
            logger.error("Erreur géocodage Google pour '{}': {}", address, e.getMessage());
            return null;
        }
    }
}
