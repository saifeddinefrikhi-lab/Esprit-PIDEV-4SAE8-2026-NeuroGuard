package com.neuroguard.consultationservice.controller;

import com.neuroguard.consultationservice.dto.*;
import com.neuroguard.consultationservice.service.DistanceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de calcul de distance entre un médecin et un patient (ou entre deux professionnels).
 * - Distance à vol d'oiseau (Haversine)
 * - Géocodage d'adresses (Nominatim ou Google)
 * - Optionnel : distance routière et temps de trajet (Google Distance Matrix)
 * - Tri des professionnels par proximité
 */
@RestController
@RequestMapping("/api/distance")
public class DistanceController {

    private final DistanceService distanceService;

    public DistanceController(DistanceService distanceService) {
        this.distanceService = distanceService;
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
}
