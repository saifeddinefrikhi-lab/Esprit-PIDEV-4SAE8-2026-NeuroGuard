package com.neuroguard.consultationservice.service;

import com.neuroguard.consultationservice.dto.DistanceMatrixResultDto;
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
 * Distance et durée par la route via Google Distance Matrix API.
 */
@Service
@ConditionalOnProperty(name = "app.distance-matrix.provider", havingValue = "google")
public class GoogleRoadDistanceService implements RoadDistanceService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleRoadDistanceService.class);
    private static final String MATRIX_URL = "https://maps.googleapis.com/maps/api/distancematrix/json";

    private final RestTemplate restTemplate;

    @Value("${app.distance-matrix.google.api-key:}")
    private String apiKey;

    public GoogleRoadDistanceService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    @SuppressWarnings("unchecked")
    public DistanceMatrixResultDto getDistanceAndDuration(GeoCoordinates origin, GeoCoordinates destination) {
        if (!isAvailable()) {
            return null;
        }
        try {
            String origins = origin.getLatitude() + "," + origin.getLongitude();
            String destinations = destination.getLatitude() + "," + destination.getLongitude();

            String url = UriComponentsBuilder.fromHttpUrl(MATRIX_URL)
                    .queryParam("origins", origins)
                    .queryParam("destinations", destinations)
                    .queryParam("key", apiKey)
                    .toUriString();

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                return null;
            }

            String status = (String) response.get("status");
            if (!"OK".equals(status)) {
                logger.warn("Google Distance Matrix status: {}", status);
                return null;
            }

            List<Map<String, Object>> rows = (List<Map<String, Object>>) response.get("rows");
            if (rows == null || rows.isEmpty()) {
                return null;
            }

            List<Map<String, Object>> elements = (List<Map<String, Object>>) rows.get(0).get("elements");
            if (elements == null || elements.isEmpty()) {
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
                return null;
            }

            int distMeters = ((Number) distance.get("value")).intValue();
            int durSeconds = ((Number) duration.get("value")).intValue();
            return new DistanceMatrixResultDto(distMeters, durSeconds);
        } catch (Exception e) {
            logger.error("Erreur Distance Matrix Google: {}", e.getMessage());
            return null;
        }
    }
}
