package com.neuroguard.consultationservice.service;

import com.neuroguard.consultationservice.dto.*;
import com.neuroguard.consultationservice.util.DistanceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour trouver les médecins les plus proches d'un patient.
 * Utilise Google Distance Matrix API pour calculs de distance réelle.
 * Supporte plusieurs modes de transport.
 */
@Service
public class FindClosestProvidersService {

    private static final Logger logger = LoggerFactory.getLogger(FindClosestProvidersService.class);

    private final GoogleRoadDistanceService roadDistanceService;
    private final UserServiceClient userServiceClient;

    public FindClosestProvidersService(GoogleRoadDistanceService roadDistanceService,
                                       UserServiceClient userServiceClient) {
        this.roadDistanceService = roadDistanceService;
        this.userServiceClient = userServiceClient;
    }

    /**
     * Trouve les médecins les plus proches du patient en utilisant Google Distance Matrix.
     * Les résultats sont triés par distance réelle.
     *
     * @param request Requête contenant localisation patient, rayon, mode transport
     * @return Réponse avec liste de médecins triés par proximité
     */
    public FindClosestProvidersResponse findClosestProviders(FindClosestProvidersRequest request) {
        long startTime = System.currentTimeMillis();
        
        logger.info("Finding closest providers for patient at {}. Mode: {}, Radius: {} km",
                request.getPatientLocation(), request.getTransportMode(), request.getRadiusKm());

        // Étape 1: Filtrer les médecins dans le rayon (Haversine rapide)
        List<ProviderLocationDto> providersInRadius = filterProvidersByRadiusHaversine(
                request.getProviders(),
                request.getPatientLocation(),
                request.getRadiusKm()
        );

        if (providersInRadius.isEmpty()) {
            FindClosestProvidersResponse response = new FindClosestProvidersResponse();
            response.setPatientLocation(request.getPatientLocation());
            response.setTransportMode(request.getTransportMode());
            response.setRadiusKm(request.getRadiusKm());
            response.setTotalFound(0);
            response.setResultCount(0);
            response.setProviders(List.of());
            response.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            response.setMessage("No providers found within " + request.getRadiusKm() + " km");
            return response;
        }

        // Étape 2: Calculer distance réelle avec Google Distance Matrix
        List<ClosestProviderDto> enricedProviders = enrichWithRealDistances(
                providersInRadius,
                request.getPatientLocation(),
                request.getTransportMode()
        );

        // Étape 3: Trier par distance réelle
        List<ClosestProviderDto> sorted = enricedProviders.stream()
                .sorted(Comparator.comparingDouble(p -> p.getRoadDistanceKm() != null ? 
                        p.getRoadDistanceKm() : p.getDistanceKm()))
                .limit(request.getLimit())
                .collect(Collectors.toList());

        long execTime = System.currentTimeMillis() - startTime;
        logger.info("Found {} providers (from {} in radius) in {} ms", 
                sorted.size(), providersInRadius.size(), execTime);

        FindClosestProvidersResponse response = new FindClosestProvidersResponse();
        response.setPatientLocation(request.getPatientLocation());
        response.setTransportMode(request.getTransportMode());
        response.setRadiusKm(request.getRadiusKm());
        response.setTotalFound(providersInRadius.size());
        response.setResultCount(sorted.size());
        response.setProviders(sorted);
        response.setExecutionTimeMs(execTime);
        response.setMessage("Found " + sorted.size() + " providers");

        return response;
    }

    /**
     * Filtre les médecins par rayon en utilisant Haversine (rapide).
     * C'est un pré-filtre avant d'appeler Google Distance Matrix sur un sous-ensemble.
     */
    private List<ProviderLocationDto> filterProvidersByRadiusHaversine(
            List<ProviderLocationDto> providers,
            GeoCoordinates referencePoint,
            Double radiusKm) {

        return providers.stream()
                .filter(provider -> {
                    double distKm = DistanceUtils.haversineKm(
                            referencePoint.getLatitude(),
                            referencePoint.getLongitude(),
                            provider.getLatitude(),
                            provider.getLongitude()
                    );
                    return distKm <= radiusKm;
                })
                .collect(Collectors.toList());
    }

    /**
     * Enrichit les médecins avec distance réelle depuis Google Distance Matrix.
     */
    private List<ClosestProviderDto> enrichWithRealDistances(
            List<ProviderLocationDto> providers,
            GeoCoordinates patientLocation,
            String transportMode) {

        return providers.parallelStream()  // Appels parallèles pour Google API
                .map(provider -> enrichProviderWithDistance(provider, patientLocation, transportMode))
                .filter(p -> p.getRoadDistanceKm() != null || p.getDistanceKm() != null)
                .collect(Collectors.toList());
    }

    /**
     * Enrichit un médecin individuel avec distance réelle via Google Distance Matrix.
     */
    private ClosestProviderDto enrichProviderWithDistance(
            ProviderLocationDto provider,
            GeoCoordinates patientLocation,
            String transportMode) {

        ClosestProviderDto dto = new ClosestProviderDto();
        dto.setProviderId(provider.getProviderId());
        dto.setLocation(new GeoCoordinates(provider.getLatitude(), provider.getLongitude()));

        // Distance Haversine (rapide, approximation)
        double haversineKm = DistanceUtils.haversineKm(
                patientLocation.getLatitude(),
                patientLocation.getLongitude(),
                provider.getLatitude(),
                provider.getLongitude()
        );
        dto.setDistanceKm(Math.round(haversineKm * 100.0) / 100.0);

        // Distance réelle via Google Distance Matrix
        try {
            DistanceMatrixResultDto matrix = roadDistanceService.getDistanceAndDuration(
                    patientLocation,
                    new GeoCoordinates(provider.getLatitude(), provider.getLongitude()),
                    transportMode
            );

            if (matrix != null) {
                double roadKm = matrix.getDistanceMeters() / 1000.0;
                dto.setRoadDistanceKm(Math.round(roadKm * 100.0) / 100.0);
                dto.setEstimatedDurationSeconds((int) matrix.getDurationSeconds());
                dto.setEstimatedDurationHuman(formatDuration((int) matrix.getDurationSeconds()));
            }
        } catch (Exception e) {
            logger.warn("Error getting distance for provider {}: {}", provider.getProviderId(), e.getMessage());
            // Fallback to Haversine distance is already set
        }

        // Tentative récupération infos supplémentaires depuis user-service
        try {
            UserDto userDto = userServiceClient.getUserById(provider.getProviderId());
            if (userDto != null) {
                dto.setProviderName(userDto.getFirstName() + " " + userDto.getLastName());
            }
        } catch (Exception e) {
            logger.debug("Could not fetch provider details from user-service: {}", e.getMessage());
        }

        return dto;
    }

    /**
     * Formate la durée en secondes dans un format lisible (ex: "15 mins", "1h 30min").
     */
    private String formatDuration(Integer seconds) {
        if (seconds == null || seconds <= 0) {
            return "N/A";
        }

        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;

        if (hours == 0) {
            return minutes + " min" + (minutes > 1 ? "s" : "");
        } else if (minutes == 0) {
            return hours + " hour" + (hours > 1 ? "s" : "");
        } else {
            return hours + "h " + minutes + "min";
        }
    }
}
