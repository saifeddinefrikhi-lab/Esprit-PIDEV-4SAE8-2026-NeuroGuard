package com.neuroguard.consultationservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Requête pour trier des professionnels par distance par rapport à un point (ex. patient).
 */
public class SortByDistanceRequest {
    @NotNull(message = "Les coordonnées du point de référence (ex. patient) sont requises")
    @Valid
    private GeoCoordinates referencePoint;

    @NotEmpty(message = "La liste des professionnels avec coordonnées est requise")
    @Valid
    private List<ProviderLocationDto> providers;

    public GeoCoordinates getReferencePoint() { return referencePoint; }
    public void setReferencePoint(GeoCoordinates referencePoint) { this.referencePoint = referencePoint; }

    public List<ProviderLocationDto> getProviders() { return providers; }
    public void setProviders(List<ProviderLocationDto> providers) { this.providers = providers; }
}
