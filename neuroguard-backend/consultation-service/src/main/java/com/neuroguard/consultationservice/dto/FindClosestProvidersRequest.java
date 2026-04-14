package com.neuroguard.consultationservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Requête pour trouver les médecins les plus proches du patient.
 * Utilise Google Distance Matrix API pour distance réelle + mode de transport.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindClosestProvidersRequest {

    /**
     * Localisation du patient (point de référence).
     */
    @NotNull(message = "Patient location is required")
    private GeoCoordinates patientLocation;

    /**
     * Mode de transport: DRIVING, WALKING, TRANSIT, BICYCLING.
     * Default: DRIVING
     */
    private String transportMode = "DRIVING";

    /**
     * Rayon de recherche en km. Default: 50 km.
     */
    @Min(value = 1, message = "Radius must be at least 1 km")
    private Double radiusKm = 50.0;

    /**
     * Liste des médecins avec leurs coordonnées.
     */
    @NotNull(message = "Providers list is required")
    private java.util.List<ProviderLocationDto> providers;

    /**
     * Limite du nombre de résultats retournés. Default: 10.
     */
    @Min(value = 1, message = "Limit must be at least 1")
    private Integer limit = 10;
}
