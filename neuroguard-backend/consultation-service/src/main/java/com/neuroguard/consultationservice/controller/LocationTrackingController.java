package com.neuroguard.consultationservice.controller;

import com.neuroguard.consultationservice.dto.LocationUpdateDto;
import com.neuroguard.consultationservice.service.LocationStreamingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * API REST pour le suivi des positions GPS en temps réel des médecins.
 * Complément aux WebSocket pour cas où HTTP polling est préféré.
 */
@RestController
@RequestMapping("/api/location")
public class LocationTrackingController {

    private final LocationStreamingService locationStreamingService;

    public LocationTrackingController(LocationStreamingService locationStreamingService) {
        this.locationStreamingService = locationStreamingService;
    }

    /**
     * Récupère la position GPS actuelle d'un médecin.
     * Retourne null si le médecin n'est pas en tournée (pas de WebSocket actif).
     *
     * @param providerId ID du médecin
     * @return Position actuelle avec latitude, longitude, timestamp
     */
    @GetMapping("/provider/{providerId}")
    @PreAuthorize("hasAnyRole('PATIENT', 'CAREGIVER', 'PROVIDER')")
    public ResponseEntity<?> getProviderLocation(@PathVariable Long providerId) {
        LocationUpdateDto location = locationStreamingService.getProviderLocation(providerId);

        if (location == null) {
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("provider_id", providerId);
                put("status", "offline");
                put("message", "Provider location not available");
            }});
        }

        Map<String, Object> response = new HashMap<>();
        response.put("provider_id", providerId);
        response.put("latitude", location.getLatitude());
        response.put("longitude", location.getLongitude());
        response.put("timestamp", location.getTimestamp());
        response.put("accuracy", location.getAccuracy());
        response.put("speed", location.getSpeed());
        response.put("status", location.getStatus());
        response.put("age_seconds", (System.currentTimeMillis() - location.getTimestamp()) / 1000.0);

        return ResponseEntity.ok(response);
    }

    /**
     * Met à jour manuellement la position d'un médecin via HTTP (fallback aux WebSocket).
     * Requête: { "latitude": 48.86, "longitude": 2.35 }
     *
     * @param providerId ID du médecin
     * @param locationData Données de position
     */
    @PostMapping("/provider/{providerId}/update")
    @PreAuthorize("hasAnyRole('PROVIDER')")
    public ResponseEntity<?> updateProviderLocation(
            @PathVariable Long providerId,
            @RequestBody Map<String, Object> locationData) {

        try {
            LocationUpdateDto update = new LocationUpdateDto();
            update.setProviderId(providerId);
            update.setLatitude(((Number) locationData.get("latitude")).doubleValue());
            update.setLongitude(((Number) locationData.get("longitude")).doubleValue());
            update.setTimestamp(System.currentTimeMillis());

            if (locationData.containsKey("accuracy")) {
                update.setAccuracy(((Number) locationData.get("accuracy")).doubleValue());
            }
            if (locationData.containsKey("altitude")) {
                update.setAltitude(((Number) locationData.get("altitude")).doubleValue());
            }
            if (locationData.containsKey("speed")) {
                update.setSpeed(((Number) locationData.get("speed")).doubleValue());
            }
            if (locationData.containsKey("status")) {
                update.setStatus((String) locationData.get("status"));
            }

            boolean stored = locationStreamingService.updateProviderLocation(update);

            if (stored) {
                return ResponseEntity.ok(new HashMap<String, Object>() {{
                    put("provider_id", providerId);
                    put("status", "success");
                    put("message", "Location updated");
                    put("timestamp", update.getTimestamp());
                }});
            } else {
                return ResponseEntity.badRequest().body(new HashMap<String, Object>() {{
                    put("status", "error");
                    put("message", "Failed to update location");
                }});
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new HashMap<String, Object>() {{
                put("status", "error");
                put("message", e.getMessage());
            }});
        }
    }

    /**
     * Récupère le statut du streaming de location pour un fournisseur.
     */
    @GetMapping("/provider/{providerId}/status")
    @PreAuthorize("hasAnyRole('PROVIDER', 'PATIENT', 'CAREGIVER')")
    public ResponseEntity<?> getProviderLocationStatus(@PathVariable Long providerId) {
        LocationUpdateDto location = locationStreamingService.getProviderLocation(providerId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("provider_id", providerId);
        response.put("is_active", location != null);
        
        if (location != null) {
            response.put("last_update", location.getTimestamp());
            response.put("age_seconds", (System.currentTimeMillis() - location.getTimestamp()) / 1000.0);
            response.put("status", location.getStatus());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Efface/arrête le tracking de position pour un médecin.
     * Appelé quand le médecin termine sa tournée.
     */
    @DeleteMapping("/provider/{providerId}")
    @PreAuthorize("hasAnyRole('PROVIDER')")
    public ResponseEntity<?> clearProviderLocation(@PathVariable Long providerId) {
        locationStreamingService.clearProviderLocation(providerId);

        return ResponseEntity.ok(new HashMap<String, Object>() {{
            put("provider_id", providerId);
            put("status", "success");
            put("message", "Location tracking stopped");
        }});
    }

    /**
     * Récupère le nombre de médecins actuellement en tournée (avec tracking actif).
     */
    @GetMapping("/stats/active-providers")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER')")
    public ResponseEntity<?> getActiveProvidersCount() {
        long count = locationStreamingService.getActiveProvidersCount();

        return ResponseEntity.ok(new HashMap<String, Object>() {{
            put("active_providers", count);
            put("timestamp", System.currentTimeMillis());
        }});
    }
}
