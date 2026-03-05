package com.neuroguard.consultationservice.controller;

import com.neuroguard.consultationservice.dto.AvailabilityRequest;
import com.neuroguard.consultationservice.dto.AvailabilityResponse;
import com.neuroguard.consultationservice.service.AvailabilityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<AvailabilityResponse> create(
            @Valid @RequestBody AvailabilityRequest request,
            @RequestAttribute("userId") Long providerId) {
        AvailabilityResponse response = availabilityService.create(providerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<AvailabilityResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AvailabilityRequest request,
            @RequestAttribute("userId") Long providerId) {
        return ResponseEntity.ok(availabilityService.update(id, request, providerId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROVIDER')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestAttribute("userId") Long providerId) {
        availabilityService.delete(id, providerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PROVIDER')")
    public List<AvailabilityResponse> getMyAvailability(
            @RequestAttribute("userId") Long providerId) {
        return availabilityService.getByProvider(providerId);
    }

    @GetMapping("/provider/{providerId}")
    @PreAuthorize("hasAnyRole('PATIENT', 'CAREGIVER', 'ADMIN')")
    public List<AvailabilityResponse> getProviderAvailability(@PathVariable Long providerId) {
        return availabilityService.getByProvider(providerId);
    }
}
