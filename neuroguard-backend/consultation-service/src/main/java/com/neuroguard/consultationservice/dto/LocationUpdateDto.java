package com.neuroguard.consultationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO pour une mise à jour de position GPS en temps réel.
 * Envoyée par l'application mobile du médecin via WebSocket.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdateDto {

    /**
     * ID du médecin.
     */
    @JsonProperty("provider_id")
    private Long providerId;

    /**
     * Latitude de la position actuelle.
     */
    @JsonProperty("latitude")
    private Double latitude;

    /**
     * Longitude de la position actuelle.
     */
    @JsonProperty("longitude")
    private Double longitude;

    /**
     * Timestamp de la mise à jour (millisecondes depuis epoch).
     */
    @JsonProperty("timestamp")
    private Long timestamp;

    /**
     * Exactitude de la position en mètres (optionnel).
     */
    @JsonProperty("accuracy")
    private Double accuracy;

    /**
     * Altitude en mètres (optionnel).
     */
    @JsonProperty("altitude")
    private Double altitude;

    /**
     * Vitesse en m/s (optionnel, signe de mouvement).
     */
    @JsonProperty("speed")
    private Double speed;

    /**
     * Statut du médecin: ACTIVE, PAUSED, COMPLETED.
     */
    @JsonProperty("status")
    private String status = "ACTIVE";

    /**
     * Token JWT pour authentification (si pas d'header Authorization).
     */
    @JsonProperty("token")
    private String token;

    /**
     * Constructeur simplifié avec infos minimales.
     */
    public LocationUpdateDto(Long providerId, Double latitude, Double longitude, Long timestamp) {
        this.providerId = providerId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.status = "ACTIVE";
    }
}
