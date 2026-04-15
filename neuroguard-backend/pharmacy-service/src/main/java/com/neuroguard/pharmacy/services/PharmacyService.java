package com.neuroguard.pharmacy.services;

import com.neuroguard.pharmacy.dto.PharmacyDto;
import com.neuroguard.pharmacy.dto.PharmacyLocationRequest;
import com.neuroguard.pharmacy.entities.Pharmacy;
import com.neuroguard.pharmacy.repositories.PharmacyRepository;
import com.neuroguard.pharmacy.utils.GeoDistanceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PharmacyService {

    private final PharmacyRepository pharmacyRepository;

    /**
     * Get all pharmacies
     */
    public List<PharmacyDto> getAllPharmacies() {
        return pharmacyRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get pharmacy by ID
     */
    public PharmacyDto getPharmacyById(Long id) {
        return pharmacyRepository.findById(id)
                .map(this::convertToDto)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found with id: " + id));
    }

    /**
     * Find nearby pharmacies for a given patient location
     * @param request Location request with patient coordinates and search radius
     * @return List of pharmacies within the specified radius, sorted by distance
     */
    public List<PharmacyDto> findNearbyPharmacies(PharmacyLocationRequest request) {
        validateLocationRequest(request);

        List<Pharmacy> pharmacies = pharmacyRepository.findPharmaciesWithinRadius(
                request.getPatientLatitude(),
                request.getPatientLongitude(),
                request.getRadiusKm()
        );

        // Filter by open status if required
        if (request.getOpenNowOnly() != null && request.getOpenNowOnly()) {
            pharmacies = pharmacies.stream()
                    .filter(p -> isPharmacyOpenNow(p))
                    .collect(Collectors.toList());
        }

        // Convert to DTO and add distance
        List<PharmacyDto> pharmacyDtos = pharmacies.stream()
                .map(pharmacy -> {
                    PharmacyDto dto = convertToDto(pharmacy);
                    double distance = GeoDistanceCalculator.calculateDistance(
                            request.getPatientLatitude(),
                            request.getPatientLongitude(),
                            pharmacy.getLatitude(),
                            pharmacy.getLongitude()
                    );
                    dto.setDistance(distance);
                    return dto;
                })
                .collect(Collectors.toList());

        // Sort by distance
        return pharmacyDtos.stream()
                .sorted(Comparator.comparingDouble(PharmacyDto::getDistance))
                .collect(Collectors.toList());
    }

    /**
     * Search pharmacies by name
     */
    public List<PharmacyDto> searchByName(String name) {
        return pharmacyRepository.findByNameContainingIgnoreCase(name)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all pharmacies that are currently open
     */
    public List<PharmacyDto> getOpenPharmacies() {
        return pharmacyRepository.findAll()
                .stream()
                .filter(this::isPharmacyOpenNow)
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get pharmacies with delivery service
     */
    public List<PharmacyDto> getPharmaciesWithDelivery() {
        return pharmacyRepository.findByHasDeliveryTrue()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get 24-hour pharmacies
     */
    public List<PharmacyDto> get24HourPharmacies() {
        return pharmacyRepository.findByAccepts24hTrue()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Create a new pharmacy (Admin only)
     */
    @Transactional
    public PharmacyDto createPharmacy(PharmacyDto pharmacyDto) {
        Pharmacy pharmacy = convertToEntity(pharmacyDto);
        pharmacy = pharmacyRepository.save(pharmacy);
        log.info("Pharmacy created with id: {}", pharmacy.getId());
        return convertToDto(pharmacy);
    }

    /**
     * Update existing pharmacy (Admin only)
     */
    @Transactional
    public PharmacyDto updatePharmacy(Long id, PharmacyDto pharmacyDto) {
        Pharmacy pharmacy = pharmacyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found with id: " + id));

        pharmacy.setName(pharmacyDto.getName());
        pharmacy.setAddress(pharmacyDto.getAddress());
        pharmacy.setPhoneNumber(pharmacyDto.getPhoneNumber());
        pharmacy.setLatitude(pharmacyDto.getLatitude());
        pharmacy.setLongitude(pharmacyDto.getLongitude());
        pharmacy.setDescription(pharmacyDto.getDescription());
        if (pharmacyDto.getOpenNow() != null) {
            pharmacy.setOpenNow(pharmacyDto.getOpenNow());
        }
        pharmacy.setOpeningTime(pharmacyDto.getOpeningTime());
        pharmacy.setClosingTime(pharmacyDto.getClosingTime());
        pharmacy.setEmail(pharmacyDto.getEmail());
        if (pharmacyDto.getHasDelivery() != null) {
            pharmacy.setHasDelivery(pharmacyDto.getHasDelivery());
        }
        if (pharmacyDto.getAccepts24h() != null) {
            pharmacy.setAccepts24h(pharmacyDto.getAccepts24h());
        }
        pharmacy.setSpecialities(pharmacyDto.getSpecialities());
        pharmacy.setImageUrl(pharmacyDto.getImageUrl());

        pharmacy = pharmacyRepository.save(pharmacy);
        log.info("Pharmacy updated with id: {}", id);
        return convertToDto(pharmacy);
    }

    /**
     * Delete pharmacy (Admin only)
     */
    @Transactional
    public void deletePharmacy(Long id) {
        if (!pharmacyRepository.existsById(id)) {
            throw new RuntimeException("Pharmacy not found with id: " + id);
        }
        pharmacyRepository.deleteById(id);
        log.info("Pharmacy deleted with id: {}", id);
    }

    /**
     * Check if pharmacy is currently open
     */
    private boolean isPharmacyOpenNow(Pharmacy pharmacy) {
        if (pharmacy.getAccepts24h()) {
            return true;
        }
        if (!pharmacy.getOpenNow()) {
            return false;
        }
        if (pharmacy.getOpeningTime() == null || pharmacy.getClosingTime() == null) {
            return true;
        }
        LocalTime now = LocalTime.now();
        return now.isAfter(pharmacy.getOpeningTime()) && now.isBefore(pharmacy.getClosingTime());
    }

    /**
     * Convert Pharmacy entity to PharmacyDto
     */
    private PharmacyDto convertToDto(Pharmacy pharmacy) {
        PharmacyDto dto = new PharmacyDto();
        dto.setId(pharmacy.getId());
        dto.setName(pharmacy.getName());
        dto.setAddress(pharmacy.getAddress());
        dto.setPhoneNumber(pharmacy.getPhoneNumber());
        dto.setLatitude(pharmacy.getLatitude());
        dto.setLongitude(pharmacy.getLongitude());
        dto.setDescription(pharmacy.getDescription());
        dto.setOpenNow(pharmacy.getOpenNow());
        dto.setOpeningTime(pharmacy.getOpeningTime());
        dto.setClosingTime(pharmacy.getClosingTime());
        dto.setEmail(pharmacy.getEmail());
        dto.setHasDelivery(pharmacy.getHasDelivery());
        dto.setAccepts24h(pharmacy.getAccepts24h());
        dto.setSpecialities(pharmacy.getSpecialities());
        dto.setImageUrl(pharmacy.getImageUrl());
        return dto;
    }

    /**
     * Convert PharmacyDto to Pharmacy entity
     */
    private Pharmacy convertToEntity(PharmacyDto dto) {
        Pharmacy pharmacy = new Pharmacy();
        if (dto.getId() != null) {
            pharmacy.setId(dto.getId());
        }
        pharmacy.setName(dto.getName());
        pharmacy.setAddress(dto.getAddress());
        pharmacy.setPhoneNumber(dto.getPhoneNumber());
        pharmacy.setLatitude(dto.getLatitude());
        pharmacy.setLongitude(dto.getLongitude());
        pharmacy.setDescription(dto.getDescription());
        pharmacy.setOpenNow(dto.getOpenNow() != null ? dto.getOpenNow() : Boolean.TRUE);
        pharmacy.setOpeningTime(dto.getOpeningTime());
        pharmacy.setClosingTime(dto.getClosingTime());
        pharmacy.setEmail(dto.getEmail());
        pharmacy.setHasDelivery(dto.getHasDelivery() != null ? dto.getHasDelivery() : Boolean.FALSE);
        pharmacy.setAccepts24h(dto.getAccepts24h() != null ? dto.getAccepts24h() : Boolean.FALSE);
        pharmacy.setSpecialities(dto.getSpecialities());
        pharmacy.setImageUrl(dto.getImageUrl());
        return pharmacy;
    }

    /**
     * Validate location request parameters
     */
    private void validateLocationRequest(PharmacyLocationRequest request) {
        if (request.getPatientLatitude() == null || request.getPatientLongitude() == null) {
            throw new IllegalArgumentException("Patient latitude and longitude are required");
        }
        if (request.getRadiusKm() == null || request.getRadiusKm() <= 0) {
            request.setRadiusKm(10); // Default radius: 10 km
        }
    }
}
