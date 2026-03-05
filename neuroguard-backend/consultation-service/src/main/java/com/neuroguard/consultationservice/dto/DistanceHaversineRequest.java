package com.neuroguard.consultationservice.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Requête pour le calcul de distance Haversine entre deux coordonnées.
 */
public class DistanceHaversineRequest {
    @NotNull(message = "Les coordonnées du premier point sont requises")
    private GeoCoordinates point1;

    @NotNull(message = "Les coordonnées du second point sont requises")
    private GeoCoordinates point2;

    public GeoCoordinates getPoint1() { return point1; }
    public void setPoint1(GeoCoordinates point1) { this.point1 = point1; }

    public GeoCoordinates getPoint2() { return point2; }
    public void setPoint2(GeoCoordinates point2) { this.point2 = point2; }
}
