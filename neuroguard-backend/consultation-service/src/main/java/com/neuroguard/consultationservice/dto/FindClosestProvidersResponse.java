package com.neuroguard.consultationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Réponse contenant une liste de médecins trouvés par recherche de proximité.
 * Les résultats sont triés par distance réelle (ou Haversine en fallback).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindClosestProvidersResponse {

    /**
     * Localisation de référence du patient.
     */
    private GeoCoordinates patientLocation;

    /**
     * Mode de transport utilisé pour le calcul.
     */
    private String transportMode;

    /**
     * Rayon de recherche utilisé (en km).
     */
    private Double radiusKm;

    /**
     * Nombre total de médecins trouvés dans le rayon.
     */
    private Integer totalFound;

    /**
     * Nombre de résultats retournés.
     */
    private Integer resultCount;

    /**
     * Liste des médecins trouvés, triés par proximité (plus proche en premier).
     */
    private List<ClosestProviderDto> providers;

    /**
     * Durée du calcul en millisecondes.
     */
    private Long executionTimeMs;

    /**
     * Message d'information ou d'erreur.
     */
    private String message;
}
