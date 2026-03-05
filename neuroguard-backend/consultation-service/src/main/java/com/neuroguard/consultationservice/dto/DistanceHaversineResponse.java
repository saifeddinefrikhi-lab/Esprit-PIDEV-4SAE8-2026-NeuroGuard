package com.neuroguard.consultationservice.dto;

/**
 * Réponse du calcul de distance Haversine (distance à vol d'oiseau en km).
 */
public class DistanceHaversineResponse {
    private double distanceKm;

    public DistanceHaversineResponse() {}

    public DistanceHaversineResponse(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
}
