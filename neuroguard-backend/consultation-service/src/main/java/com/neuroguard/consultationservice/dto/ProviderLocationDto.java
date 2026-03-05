package com.neuroguard.consultationservice.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Représente un professionnel (médecin, etc.) avec ses coordonnées pour le tri par proximité.
 */
public class ProviderLocationDto {
    @NotNull(message = "L'identifiant du professionnel est requis")
    private Long providerId;

    @NotNull(message = "La latitude est requise")
    private Double latitude;

    @NotNull(message = "La longitude est requise")
    private Double longitude;

    public Long getProviderId() { return providerId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
