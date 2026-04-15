package com.neuroguard.pharmacy.controllers;

import com.neuroguard.pharmacy.dto.ClinicDto;
import com.neuroguard.pharmacy.dto.PharmacyDto;
import com.neuroguard.pharmacy.dto.PharmacyLocationRequest;
import com.neuroguard.pharmacy.services.ClinicService;
import com.neuroguard.pharmacy.services.PharmacyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pharmacies")
@RequiredArgsConstructor
@Slf4j
public class PharmacyController {

    private final PharmacyService pharmacyService;
    private final ClinicService clinicService;

    /**
     * Get all pharmacies
     */
    @GetMapping
    public ResponseEntity<List<PharmacyDto>> getAllPharmacies() {
        log.info("Fetching all pharmacies");
        return ResponseEntity.ok(pharmacyService.getAllPharmacies());
    }

    /**
     * Get pharmacy by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<PharmacyDto> getPharmacyById(@PathVariable Long id) {
        log.info("Fetching pharmacy with id: {}", id);
        return ResponseEntity.ok(pharmacyService.getPharmacyById(id));
    }

    /**
     * Find nearby pharmacies for a given patient location
     * POST /api/pharmacies/nearby
     * Body: { "patientLatitude": 48.8566, "patientLongitude": 2.3522, "radiusKm": 10, "openNowOnly": true }
     */
    @PostMapping("/nearby")
    public ResponseEntity<List<PharmacyDto>> findNearbyPharmacies(
            @RequestBody PharmacyLocationRequest request) {
        log.info("Finding pharmacies near location: {}, {}", request.getPatientLatitude(), request.getPatientLongitude());
        return ResponseEntity.ok(pharmacyService.findNearbyPharmacies(request));
    }

    /**
     * Search pharmacies by name
     */
    @GetMapping("/search")
    public ResponseEntity<List<PharmacyDto>> searchByName(@RequestParam String name) {
        log.info("Searching pharmacies by name: {}", name);
        return ResponseEntity.ok(pharmacyService.searchByName(name));
    }

    /**
     * Get all currently open pharmacies
     */
    @GetMapping("/open")
    public ResponseEntity<List<PharmacyDto>> getOpenPharmacies() {
        log.info("Fetching open pharmacies");
        return ResponseEntity.ok(pharmacyService.getOpenPharmacies());
    }

    /**
     * Get pharmacies with delivery service
     */
    @GetMapping("/delivery")
    public ResponseEntity<List<PharmacyDto>> getPharmaciesWithDelivery() {
        log.info("Fetching pharmacies with delivery service");
        return ResponseEntity.ok(pharmacyService.getPharmaciesWithDelivery());
    }

    /**
     * Get 24-hour pharmacies
     */
    @GetMapping("/24hours")
    public ResponseEntity<List<PharmacyDto>> get24HourPharmacies() {
        log.info("Fetching 24-hour pharmacies");
        return ResponseEntity.ok(pharmacyService.get24HourPharmacies());
    }

    /**
     * Create a new pharmacy (Admin only)
     */
    @PostMapping
    public ResponseEntity<PharmacyDto> createPharmacy(@RequestBody PharmacyDto pharmacyDto) {
        log.info("Creating new pharmacy: {}", pharmacyDto.getName());
        PharmacyDto created = pharmacyService.createPharmacy(pharmacyDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update pharmacy (Admin only)
     */
    @PutMapping("/{id}")
    public ResponseEntity<PharmacyDto> updatePharmacy(
            @PathVariable Long id,
            @RequestBody PharmacyDto pharmacyDto) {
        log.info("Updating pharmacy with id: {}", id);
        return ResponseEntity.ok(pharmacyService.updatePharmacy(id, pharmacyDto));
    }

    /**
     * Delete pharmacy (Admin only)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePharmacy(@PathVariable Long id) {
        log.info("Deleting pharmacy with id: {}", id);
        pharmacyService.deletePharmacy(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Pharmacy Service is running");
    }

    @GetMapping("/clinics")
    public ResponseEntity<List<ClinicDto>> getAllClinics() {
        log.info("Fetching all clinics");
        return ResponseEntity.ok(clinicService.getAllClinics());
    }

    @GetMapping("/clinics/{id}")
    public ResponseEntity<ClinicDto> getClinicById(@PathVariable Long id) {
        log.info("Fetching clinic with id: {}", id);
        return ResponseEntity.ok(clinicService.getClinicById(id));
    }

    @PostMapping("/clinics/nearby")
    public ResponseEntity<List<ClinicDto>> findNearbyClinics(@RequestBody PharmacyLocationRequest request) {
        log.info("Finding clinics near location: {}, {}", request.getPatientLatitude(), request.getPatientLongitude());
        return ResponseEntity.ok(clinicService.findNearbyClinics(request));
    }

    @GetMapping("/clinics/search")
    public ResponseEntity<List<ClinicDto>> searchClinicsByName(@RequestParam String name) {
        log.info("Searching clinics by name: {}", name);
        return ResponseEntity.ok(clinicService.searchByName(name));
    }

    @GetMapping("/clinics/emergency")
    public ResponseEntity<List<ClinicDto>> getEmergencyClinics() {
        return ResponseEntity.ok(clinicService.getEmergencyClinics());
    }

    @GetMapping("/clinics/insurance")
    public ResponseEntity<List<ClinicDto>> getInsuranceClinics() {
        return ResponseEntity.ok(clinicService.getInsuranceClinics());
    }

    @PostMapping("/clinics")
    public ResponseEntity<ClinicDto> createClinic(@RequestBody ClinicDto clinicDto) {
        ClinicDto created = clinicService.createClinic(clinicDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/clinics/{id}")
    public ResponseEntity<ClinicDto> updateClinic(@PathVariable Long id, @RequestBody ClinicDto clinicDto) {
        return ResponseEntity.ok(clinicService.updateClinic(id, clinicDto));
    }

    @DeleteMapping("/clinics/{id}")
    public ResponseEntity<Void> deleteClinic(@PathVariable Long id) {
        clinicService.deleteClinic(id);
        return ResponseEntity.noContent().build();
    }
}
