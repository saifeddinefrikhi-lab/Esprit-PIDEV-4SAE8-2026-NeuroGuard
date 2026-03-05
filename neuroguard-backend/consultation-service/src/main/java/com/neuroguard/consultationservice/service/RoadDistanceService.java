package com.neuroguard.consultationservice.service;

import com.neuroguard.consultationservice.dto.DistanceMatrixResultDto;
import com.neuroguard.consultationservice.dto.GeoCoordinates;

/**
 * Service pour obtenir la distance routière et la durée de trajet entre deux points.
 * Optionnel (ex. Google Distance Matrix API).
 */
public interface RoadDistanceService {

    /**
     * Indique si le service est disponible (ex. clé API configurée).
     */
    boolean isAvailable();

    /**
     * Calcule la distance routière et la durée entre origine et destination.
     *
     * @param origin      Coordonnées de l'origine
     * @param destination Coordonnées de la destination
     * @return Résultat (distance en mètres, durée en secondes) ou null si indisponible
     */
    DistanceMatrixResultDto getDistanceAndDuration(GeoCoordinates origin, GeoCoordinates destination);
}
