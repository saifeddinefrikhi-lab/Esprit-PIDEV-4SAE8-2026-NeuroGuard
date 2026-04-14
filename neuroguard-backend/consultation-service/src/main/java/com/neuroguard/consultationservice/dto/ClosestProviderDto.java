package com.neuroguard.consultationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Réponse pour un médecin trouvé par recherche de proximité.
 * Inclut distance réelle, durée de trajet (ETA), et coordonnées.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClosestProviderDto {

    /**
     * ID du médecin.
     */
    private Long providerId;

    /**
     * Nom du médecin (optionnel, envoyé par le client).
     */
    private String providerName;

    /**
     * Spécialité du médecin (optionnel).
     */
    private String specialty;

    /**
     * Localisation du médecin.
     */
    private GeoCoordinates location;

    /**
     * Distance en km (calculée à vol d'oiseau par Haversine - approximation rapide).
     */
    private Double distanceKm;

    /**
     * Distance réelle en km (via Google Distance Matrix API).
     */
    private Double roadDistanceKm;

    /**
     * Durée estimée du trajet en secondes (ETA).
     */
    private Integer estimatedDurationSeconds;

    /**
     * Représentation lisible de la durée (ex: "15 mins", "1h 30mins").
     */
    private String estimatedDurationHuman;

    /**
     * Disponibilité du médecin (optionnel: AVAILABLE, BUSY, OFFLINE).
     */
    private String availability;

    /**
     * Note/Rating du médecin (optionnel, 0-5).
     */
    private Double rating;

    /**
     * Nombre d'avis (optionnel).
     */
    private Integer reviewCount;
}
