package com.neuroguard.consultationservice.service;

import com.neuroguard.consultationservice.dto.GeoCoordinates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Géocodage via OpenStreetMap Nominatim (gratuit, sans clé API).
 * Respecter la politique d'utilisation : max 1 requête/seconde, envoyer un User-Agent identifiant l'application.
 */
@Service
@ConditionalOnProperty(name = "app.geocoding.provider", havingValue = "nominatim", matchIfMissing = true)
public class NominatimGeocodingService implements GeocodingService {

    private static final Logger logger = LoggerFactory.getLogger(NominatimGeocodingService.class);
    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";

    private final RestTemplate restTemplate;

    @Value("${app.geocoding.nominatim.user-agent:NeuroGuard-ConsultationService/1.0}")
    private String userAgent;

    public NominatimGeocodingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public GeoCoordinates geocode(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }
        try {
            String encoded = URLEncoder.encode(address.trim(), StandardCharsets.UTF_8);
            String url = NOMINATIM_URL + "?q=" + encoded + "&format=json&limit=1";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", userAgent);

            ResponseEntity<Map[]> response = restTemplate.exchange(
                    URI.create(url),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map[].class
            );

            if (response.getBody() == null || response.getBody().length == 0) {
                logger.warn("Aucun résultat Nominatim pour l'adresse: {}", address);
                return null;
            }

            Map<String, Object> first = response.getBody()[0];
            String latStr = (String) first.get("lat");
            String lonStr = (String) first.get("lon");
            if (latStr == null || lonStr == null) {
                return null;
            }

            double lat = Double.parseDouble(latStr);
            double lon = Double.parseDouble(lonStr);
            return new GeoCoordinates(lat, lon);
        } catch (Exception e) {
            logger.error("Erreur géocodage Nominatim pour '{}': {}", address, e.getMessage());
            return null;
        }
    }
}
