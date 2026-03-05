package com.neuroguard.consultationservice.service;

import com.neuroguard.consultationservice.dto.*;
import com.neuroguard.consultationservice.exception.ResourceNotFoundException;
import com.neuroguard.consultationservice.util.DistanceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Service principal pour le calcul des distances (Haversine, géocodage, optionnellement distance routière).
 */
@Service
public class DistanceService {

    private static final Logger logger = LoggerFactory.getLogger(DistanceService.class);

    private final GeocodingService geocodingService;
    private final RoadDistanceService roadDistanceService;

    public DistanceService(GeocodingService geocodingService, RoadDistanceService roadDistanceService) {
        this.geocodingService = geocodingService;
        this.roadDistanceService = roadDistanceService;
    }

    /**
     * Distance à vol d'oiseau (Haversine) entre deux coordonnées, en km.
     */
    public double haversineKm(GeoCoordinates point1, GeoCoordinates point2) {
        return DistanceUtils.haversineKm(
                point1.getLatitude(), point1.getLongitude(),
                point2.getLatitude(), point2.getLongitude()
        );
    }

    /**
     * Réponse pour un calcul Haversine entre deux points.
     */
    public DistanceHaversineResponse computeHaversine(DistanceHaversineRequest request) {
        double km = haversineKm(request.getPoint1(), request.getPoint2());
        return new DistanceHaversineResponse(Math.round(km * 100.0) / 100.0);
    }

    /**
     * Géocode une adresse et retourne les coordonnées.
     */
    public GeoCoordinates geocode(String address) {
        GeoCoordinates coords = geocodingService.geocode(address);
        if (coords == null) {
            throw new ResourceNotFoundException("Impossible de géocoder l'adresse : " + address);
        }
        return coords;
    }

    /**
     * Calcule la distance entre deux adresses (géocodage puis Haversine).
     * Si une API Distance Matrix est configurée, remplit aussi distance routière et durée.
     */
    public DistanceFromAddressesResponse computeDistanceFromAddresses(DistanceFromAddressesRequest request) {
        GeoCoordinates coords1 = geocodingService.geocode(request.getAddress1());
        GeoCoordinates coords2 = geocodingService.geocode(request.getAddress2());

        if (coords1 == null) {
            throw new ResourceNotFoundException("Impossible de géocoder l'adresse 1 : " + request.getAddress1());
        }
        if (coords2 == null) {
            throw new ResourceNotFoundException("Impossible de géocoder l'adresse 2 : " + request.getAddress2());
        }

        double distanceKm = haversineKm(coords1, coords2);
        distanceKm = Math.round(distanceKm * 100.0) / 100.0;

        DistanceFromAddressesResponse response = new DistanceFromAddressesResponse();
        response.setCoordinates1(coords1);
        response.setCoordinates2(coords2);
        response.setDistanceKm(distanceKm);

        if (roadDistanceService.isAvailable()) {
            DistanceMatrixResultDto matrix = roadDistanceService.getDistanceAndDuration(coords1, coords2);
            if (matrix != null) {
                response.setDistanceMeters(matrix.getDistanceMeters());
                response.setDurationSeconds(matrix.getDurationSeconds());
            }
        }

        return response;
    }

    /**
     * Trie les professionnels par distance (Haversine) par rapport à un point de référence.
     * Retourne la liste des providerId avec leur distance en km, triée par proximité.
     */
    public List<ProviderWithDistanceDto> sortProvidersByDistance(SortByDistanceRequest request) {
        double refLat = request.getReferencePoint().getLatitude();
        double refLon = request.getReferencePoint().getLongitude();

        return request.getProviders().stream()
                .map(p -> {
                    double km = DistanceUtils.haversineKm(refLat, refLon, p.getLatitude(), p.getLongitude());
                    return new ProviderWithDistanceDto(p.getProviderId(), Math.round(km * 100.0) / 100.0);
                })
                .sorted(Comparator.comparingDouble(ProviderWithDistanceDto::getDistanceKm))
                .toList();
    }
}
