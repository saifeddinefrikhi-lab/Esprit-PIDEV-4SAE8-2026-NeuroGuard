package com.neuroguard.consultationservice.dto;

/**
 * Professionnel avec sa distance (en km) par rapport à un point de référence.
 */
public class ProviderWithDistanceDto {
    private Long providerId;
    private double distanceKm;

    public ProviderWithDistanceDto() {}

    public ProviderWithDistanceDto(Long providerId, double distanceKm) {
        this.providerId = providerId;
        this.distanceKm = distanceKm;
    }

    public Long getProviderId() { return providerId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
}
