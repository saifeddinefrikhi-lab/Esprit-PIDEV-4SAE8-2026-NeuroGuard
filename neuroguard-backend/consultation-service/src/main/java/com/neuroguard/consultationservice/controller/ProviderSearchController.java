package com.neuroguard.consultationservice.controller;

import com.neuroguard.consultationservice.dto.*;
import com.neuroguard.consultationservice.service.FindClosestProvidersService;
import com.neuroguard.consultationservice.service.UserServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Endpoint avancé : trouve les médecins les plus proches d'un patient au moment du login.
 *
 * GET /api/providers/nearest?lat=48.86&lon=2.35&radius=50&mode=DRIVING&limit=10
 *
 * Flux :
 * 1. Récupère tous les PROVIDER depuis user-service (via Feign)
 * 2. Filtre ceux qui ont des coordonnées GPS définies
 * 3. Délègue à FindClosestProvidersService pour calculer distances + ETA
 * 4. Retourne la liste triée, plus proche en premier
 */
@RestController
@RequestMapping("/api/providers")
public class ProviderSearchController {

    private static final Logger logger = LoggerFactory.getLogger(ProviderSearchController.class);

    private final UserServiceClient userServiceClient;
    private final FindClosestProvidersService findClosestProvidersService;

    public ProviderSearchController(UserServiceClient userServiceClient,
                                    FindClosestProvidersService findClosestProvidersService) {
        this.userServiceClient = userServiceClient;
        this.findClosestProvidersService = findClosestProvidersService;
    }

    /**
     * Trouve les médecins les plus proches d'un patient.
     *
     * @param lat       Latitude du patient
     * @param lon       Longitude du patient
     * @param radius    Rayon de recherche en km (défaut: 50)
     * @param mode      Mode de transport: DRIVING, WALKING, TRANSIT, BICYCLING (défaut: DRIVING)
     * @param limit     Nombre max de résultats (défaut: 10)
     * @return Liste des médecins triés par proximité
     */
    @GetMapping("/nearest")
    @PreAuthorize("hasAnyRole('PATIENT', 'CAREGIVER', 'PROVIDER', 'ADMIN')")
    public ResponseEntity<FindClosestProvidersResponse> findNearestProviders(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "50") double radius,
            @RequestParam(defaultValue = "DRIVING") String mode,
            @RequestParam(defaultValue = "10") int limit) {

        logger.info("Recherche médecins proches: lat={}, lon={}, rayon={}km, mode={}", lat, lon, radius, mode);

        // Étape 1 : Récupérer tous les providers depuis user-service
        List<UserDto> allProviders;
        try {
            allProviders = userServiceClient.getUsersByRole("PROVIDER");
        } catch (Exception e) {
            logger.error("Impossible de récupérer les providers depuis user-service: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }

        if (allProviders == null || allProviders.isEmpty()) {
            FindClosestProvidersResponse empty = new FindClosestProvidersResponse();
            empty.setPatientLocation(new GeoCoordinates(lat, lon));
            empty.setTransportMode(mode);
            empty.setRadiusKm(radius);
            empty.setTotalFound(0);
            empty.setResultCount(0);
            empty.setProviders(List.of());
            empty.setExecutionTimeMs(0L);
            empty.setMessage("Aucun médecin trouvé dans la base de données");
            return ResponseEntity.ok(empty);
        }

        // Étape 2 : Convertir en ProviderLocationDto (filtrer ceux sans coordonnées)
        List<ProviderLocationDto> providerLocations = allProviders.stream()
                .filter(u -> u.getLatitude() != null && u.getLongitude() != null)
                .map(u -> {
                    ProviderLocationDto dto = new ProviderLocationDto();
                    dto.setProviderId(u.getId());
                    dto.setLatitude(u.getLatitude());
                    dto.setLongitude(u.getLongitude());
                    return dto;
                })
                .collect(Collectors.toList());

        logger.info("{} providers avec coordonnées GPS sur {} total", providerLocations.size(), allProviders.size());

        // Étape 3 : Si aucun provider n'a de coordonnées, générer des positions fictives proches
        // (pour permettre la démo sans données GPS réelles)
        if (providerLocations.isEmpty()) {
            logger.warn("Aucun provider n'a de coordonnées GPS. Utilisation de positions de démonstration.");
            providerLocations = generateDemoProviderLocations(allProviders, lat, lon);
        }

        // Étape 4 : Calculer les distances via FindClosestProvidersService
        GeoCoordinates patientLocation = new GeoCoordinates(lat, lon);
        FindClosestProvidersRequest request = new FindClosestProvidersRequest(
                patientLocation, mode, radius, providerLocations, limit
        );

        FindClosestProvidersResponse response = findClosestProvidersService.findClosestProviders(request);

        // Enrichir les noms des providers depuis allProviders
        if (response.getProviders() != null) {
            response.getProviders().forEach(provider -> {
                allProviders.stream()
                        .filter(u -> u.getId().equals(provider.getProviderId()))
                        .findFirst()
                        .ifPresent(u -> {
                            if (provider.getProviderName() == null || provider.getProviderName().isBlank()) {
                                provider.setProviderName("Dr. " + u.getFirstName() + " " + u.getLastName());
                            }
                        });
            });
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Génère des positions de démonstration autour du patient si les providers n'ont pas de GPS.
     * Utile pour les environnements de test/démo.
     */
    private List<ProviderLocationDto> generateDemoProviderLocations(
            List<UserDto> providers, double patLat, double patLon) {

        double[] latOffsets = {0.01, -0.02, 0.03, -0.01, 0.02, -0.03, 0.015, -0.025};
        double[] lonOffsets = {0.02, 0.01, -0.01, -0.02, 0.025, 0.005, -0.015, 0.03};

        List<ProviderLocationDto> result = new java.util.ArrayList<>();
        for (int i = 0; i < providers.size() && i < latOffsets.length; i++) {
            ProviderLocationDto dto = new ProviderLocationDto();
            dto.setProviderId(providers.get(i).getId());
            dto.setLatitude(patLat + latOffsets[i]);
            dto.setLongitude(patLon + lonOffsets[i]);
            result.add(dto);
        }
        return result;
    }
}
