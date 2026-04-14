package com.neuroguard.consultationservice.controller;

import com.neuroguard.consultationservice.dto.*;
import com.neuroguard.consultationservice.service.DistanceService;
import com.neuroguard.consultationservice.service.FindClosestProvidersService;
import com.neuroguard.consultationservice.service.OptimizeRouteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API de calcul de distance entre un médecin et un patient (ou entre deux professionnels).
 * - Distance à vol d'oiseau (Haversine)
 * - Géocodage d'adresses (Nominatim ou Google)
 * - Optionnel : distance routière et temps de trajet (Google Distance Matrix)
 * - Tri des professionnels par proximité
 * - Nouvelle fonctionnalité avancée: recherche médecin le plus proche + optimisation tournée
 */
@RestController
@RequestMapping("/api/distance")
public class DistanceController {

    private final DistanceService distanceService;
    private final FindClosestProvidersService findClosestProvidersService;
    private final OptimizeRouteService optimizeRouteService;

    public DistanceController(DistanceService distanceService,
                             FindClosestProvidersService findClosestProvidersService,
                             OptimizeRouteService optimizeRouteService) {
        this.distanceService = distanceService;
        this.findClosestProvidersService = findClosestProvidersService;
        this.optimizeRouteService = optimizeRouteService;
    }

    /**
     * Calcule la distance à vol d'oiseau (Haversine) entre deux coordonnées.
     * Corps : { "point1": { "latitude": 48.86, "longitude": 2.35 }, "point2": { "latitude": 48.90, "longitude": 2.40 } }
     */
    @PostMapping("/haversine")
    @PreAuthorize("hasAnyRole('PROVIDER', 'PATIENT', 'CAREGIVER')")
    public ResponseEntity<DistanceHaversineResponse> haversine(@Valid @RequestBody DistanceHaversineRequest request) {
        DistanceHaversineResponse response = distanceService.computeHaversine(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Géocode une adresse et retourne les coordonnées (latitude, longitude).
     */
    @GetMapping("/geocode")
    @PreAuthorize("hasAnyRole('PROVIDER', 'PATIENT', 'CAREGIVER')")
    public ResponseEntity<GeoCoordinates> geocode(@RequestParam String address) {
        GeoCoordinates coords = distanceService.geocode(address);
        return ResponseEntity.ok(coords);
    }

    /**
     * Calcule la distance entre deux adresses (géocodage puis Haversine).
     * Si une API Distance Matrix est configurée, renvoie aussi la distance routière et la durée.
     * Corps : { "address1": "10 rue de la Paix, Paris", "address2": "Place de la Bastille, Paris" }
     */
    @PostMapping("/from-addresses")
    @PreAuthorize("hasAnyRole('PROVIDER', 'PATIENT', 'CAREGIVER')")
    public ResponseEntity<DistanceFromAddressesResponse> fromAddresses(@Valid @RequestBody DistanceFromAddressesRequest request) {
        DistanceFromAddressesResponse response = distanceService.computeDistanceFromAddresses(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Trie des professionnels par distance par rapport à un point de référence (ex. coordonnées du patient).
     * Retourne la liste des providerId avec distance en km, du plus proche au plus loin.
     * Corps : { "referencePoint": { "latitude": 48.86, "longitude": 2.35 }, "providers": [ { "providerId": 1, "latitude": 48.90, "longitude": 2.40 }, ... ] }
     */
    @PostMapping("/sort-by-distance")
    @PreAuthorize("hasAnyRole('PROVIDER', 'PATIENT', 'CAREGIVER')")
    public ResponseEntity<List<ProviderWithDistanceDto>> sortByDistance(@Valid @RequestBody SortByDistanceRequest request) {
        List<ProviderWithDistanceDto> sorted = distanceService.sortProvidersByDistance(request);
        return ResponseEntity.ok(sorted);
    }

    /**
     * ========== NOUVELLE FONCTIONNALITÉ AVANCÉE ==========
     * Trouve les médecins les plus proches du patient.
     * Utilise Google Distance Matrix pour distance réelle (pas VOL D'OISEAU).
     * Supporte multiples modes de transport: DRIVING, WALKING, TRANSIT, BICYCLING.
     * 
     * Requête:
     * {
     *   "patientLocation": { "latitude": 48.86, "longitude": 2.35 },
     *   "transportMode": "DRIVING",  // ou WALKING, TRANSIT, BICYCLING
     *   "radiusKm": 50,
     *   "providers": [ { "providerId": 1, "latitude": 48.90, "longitude": 2.40 }, ... ],
     *   "limit": 10
     * }
     * 
     * Réponse: Liste de médecins avec distance réelle + ETA du trajet
     */
    @PostMapping("/find-closest-providers")
    @PreAuthorize("hasAnyRole('PATIENT', 'CAREGIVER', 'PROVIDER')")
    public ResponseEntity<FindClosestProvidersResponse> findClosestProviders(
            @Valid @RequestBody FindClosestProvidersRequest request) {
        FindClosestProvidersResponse response = findClosestProvidersService.findClosestProviders(request);
        return ResponseEntity.ok(response);
    }

    /**
     * ========== NOUVELLE FONCTIONNALITÉ: OPTIMISATION DE TOURNÉE ==========
     * Optimise l'ordre de visite de plusieurs patients pour un médecin (Traveling Salesman Problem).
     * Utilise algorithme Nearest Neighbor (rapide, qualité décente pour N < 50 patients).
     * 
     * Requête:
     * {
     *   "startLocation": { "latitude": 48.86, "longitude": 2.35 },  // Localisation du médecin
     *   "patientLocations": [ 
     *     { "latitude": 48.90, "longitude": 2.40 },
     *     { "latitude": 48.92, "longitude": 2.42 }
     *   ],
     *   "patientLabels": [ "Patient Jean", "Patient Marie" ]
     * }
     * 
     * Réponse: Ordre optimal, distance totale, durée estimée
     */
    @PostMapping("/optimize-tour")
    @PreAuthorize("hasAnyRole('PROVIDER')")
    public ResponseEntity<?> optimizeTour(@RequestBody Map<String, Object> request) {
        try {
            // Parse la requête manuelle pour flexibilité
            Double startLat = ((Number) ((Map) request.get("startLocation")).get("latitude")).doubleValue();
            Double startLon = ((Number) ((Map) request.get("startLocation")).get("longitude")).doubleValue();
            GeoCoordinates startLocation = new GeoCoordinates(startLat, startLon);

            List<Map<String, Double>> patientMaps = (List<Map<String, Double>>) request.get("patientLocations");
            List<GeoCoordinates> patientLocations = patientMaps.stream()
                    .map(m -> new GeoCoordinates(m.get("latitude"), m.get("longitude")))
                    .toList();

            List<String> patientLabels = (List<String>) request.get("patientLabels");

            OptimizeRouteService.OptimizedRoute result = optimizeRouteService.optimizeRoute(
                    startLocation, patientLocations, patientLabels);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Version avancée d'optimisation avec amélioration 2-Opt.
     * Meilleure qualité mais légèrement plus lent.
     */
    @PostMapping("/optimize-tour-advanced")
    @PreAuthorize("hasAnyRole('PROVIDER')")
    public ResponseEntity<?> optimizeTourAdvanced(@RequestBody Map<String, Object> request) {
        try {
            Double startLat = ((Number) ((Map) request.get("startLocation")).get("latitude")).doubleValue();
            Double startLon = ((Number) ((Map) request.get("startLocation")).get("longitude")).doubleValue();
            GeoCoordinates startLocation = new GeoCoordinates(startLat, startLon);

            List<Map<String, Double>> patientMaps = (List<Map<String, Double>>) request.get("patientLocations");
            List<GeoCoordinates> patientLocations = patientMaps.stream()
                    .map(m -> new GeoCoordinates(m.get("latitude"), m.get("longitude")))
                    .toList();

            List<String> patientLabels = (List<String>) request.get("patientLabels");

            OptimizeRouteService.OptimizedRoute result = optimizeRouteService.optimizeRouteWith2Opt(
                    startLocation, patientLocations, patientLabels);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}

