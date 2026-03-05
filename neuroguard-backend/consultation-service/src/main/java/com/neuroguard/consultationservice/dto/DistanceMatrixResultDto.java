package com.neuroguard.consultationservice.dto;

/**
 * Résultat d'un calcul Distance Matrix (distance routière et durée).
 */
public class DistanceMatrixResultDto {
    private long distanceMeters;
    private long durationSeconds;

    public DistanceMatrixResultDto() {}

    public DistanceMatrixResultDto(long distanceMeters, long durationSeconds) {
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
    }

    public long getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(long distanceMeters) { this.distanceMeters = distanceMeters; }

    public long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }
}
