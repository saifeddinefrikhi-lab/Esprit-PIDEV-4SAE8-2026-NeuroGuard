package com.neuroguard.consultationservice.service;

import com.neuroguard.consultationservice.dto.GeoCoordinates;

/**
 * Service de géocodage : conversion d'une adresse en coordonnées (latitude, longitude).
 */
public interface GeocodingService {

    /**
     * Convertit une adresse en coordonnées géographiques.
     *
     * @param address Adresse à géocoder (ex. "10 rue de la Paix, Paris, France")
     * @return Coordonnées ou null si l'adresse n'a pas pu être géocodée
     */
    GeoCoordinates geocode(String address);
}
