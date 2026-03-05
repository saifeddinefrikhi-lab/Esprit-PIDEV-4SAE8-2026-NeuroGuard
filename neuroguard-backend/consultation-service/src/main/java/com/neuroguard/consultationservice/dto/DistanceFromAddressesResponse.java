package com.neuroguard.consultationservice.dto;

/**
 * Réponse du calcul de distance entre deux adresses (coordonnées géocodées + distance Haversine).
 * Optionnellement distance routière et durée si Distance Matrix est configurée.
 */
public class DistanceFromAddressesResponse {
    private GeoCoordinates coordinates1;
    private GeoCoordinates coordinates2;
    private double distanceKm;
    private Long distanceMeters;       // distance routière en mètres (optionnel)
    private Long durationSeconds;      // durée du trajet en secondes (optionnel)

    public GeoCoordinates getCoordinates1() { return coordinates1; }
    public void setCoordinates1(GeoCoordinates coordinates1) { this.coordinates1 = coordinates1; }

    public GeoCoordinates getCoordinates2() { return coordinates2; }
    public void setCoordinates2(GeoCoordinates coordinates2) { this.coordinates2 = coordinates2; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public Long getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(Long distanceMeters) { this.distanceMeters = distanceMeters; }

    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }
}
