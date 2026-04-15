package com.neuroguard.pharmacy.services;

import com.neuroguard.pharmacy.dto.ClinicDto;
import com.neuroguard.pharmacy.dto.PharmacyLocationRequest;
import com.neuroguard.pharmacy.entities.Clinic;
import com.neuroguard.pharmacy.repositories.ClinicRepository;
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
public class ClinicService {

    private final ClinicRepository clinicRepository;

    public List<ClinicDto> getAllClinics() {
        return clinicRepository.findAll().stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public ClinicDto getClinicById(Long id) {
        return clinicRepository.findById(id)
                .map(this::convertToDto)
                .orElseThrow(() -> new RuntimeException("Clinic not found with id: " + id));
    }

    public List<ClinicDto> searchByName(String name) {
        return clinicRepository.findByNameContainingIgnoreCase(name).stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public List<ClinicDto> findNearbyClinics(PharmacyLocationRequest request) {
        validateLocationRequest(request);

        List<Clinic> clinics = clinicRepository.findClinicsWithinRadius(
                request.getPatientLatitude(),
                request.getPatientLongitude(),
                request.getRadiusKm()
        );

        if (request.getOpenNowOnly() != null && request.getOpenNowOnly()) {
            clinics = clinics.stream().filter(this::isClinicOpenNow).collect(Collectors.toList());
        }

        return clinics.stream()
                .map(clinic -> {
                    ClinicDto dto = convertToDto(clinic);
                    dto.setDistance(GeoDistanceCalculator.calculateDistance(
                            request.getPatientLatitude(),
                            request.getPatientLongitude(),
                            clinic.getLatitude(),
                            clinic.getLongitude()
                    ));
                    return dto;
                })
                .sorted(Comparator.comparingDouble(c -> c.getDistance() != null ? c.getDistance() : 9999d))
                .collect(Collectors.toList());
    }

    public List<ClinicDto> getEmergencyClinics() {
        return clinicRepository.findByEmergencyServiceTrue().stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public List<ClinicDto> getInsuranceClinics() {
        return clinicRepository.findByAcceptsInsuranceTrue().stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Transactional
    public ClinicDto createClinic(ClinicDto clinicDto) {
        Clinic clinic = convertToEntity(clinicDto);
        clinic = clinicRepository.save(clinic);
        log.info("Clinic created with id: {}", clinic.getId());
        return convertToDto(clinic);
    }

    @Transactional
    public ClinicDto updateClinic(Long id, ClinicDto clinicDto) {
        Clinic clinic = clinicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Clinic not found with id: " + id));

        clinic.setName(clinicDto.getName());
        clinic.setAddress(clinicDto.getAddress());
        clinic.setPhoneNumber(clinicDto.getPhoneNumber());
        clinic.setLatitude(clinicDto.getLatitude());
        clinic.setLongitude(clinicDto.getLongitude());
        clinic.setDescription(clinicDto.getDescription());
        clinic.setEmail(clinicDto.getEmail());
        clinic.setOpenNow(clinicDto.getOpenNow());
        clinic.setOpeningTime(clinicDto.getOpeningTime());
        clinic.setClosingTime(clinicDto.getClosingTime());
        clinic.setEmergencyService(clinicDto.getEmergencyService());
        clinic.setAcceptsInsurance(clinicDto.getAcceptsInsurance());
        clinic.setSpecialities(clinicDto.getSpecialities());
        clinic.setImageUrl(clinicDto.getImageUrl());

        clinic = clinicRepository.save(clinic);
        log.info("Clinic updated with id: {}", id);
        return convertToDto(clinic);
    }

    @Transactional
    public void deleteClinic(Long id) {
        if (!clinicRepository.existsById(id)) {
            throw new RuntimeException("Clinic not found with id: " + id);
        }
        clinicRepository.deleteById(id);
        log.info("Clinic deleted with id: {}", id);
    }

    private boolean isClinicOpenNow(Clinic clinic) {
        if (!Boolean.TRUE.equals(clinic.getOpenNow())) {
            return false;
        }
        if (clinic.getOpeningTime() == null || clinic.getClosingTime() == null) {
            return true;
        }
        LocalTime now = LocalTime.now();
        return now.isAfter(clinic.getOpeningTime()) && now.isBefore(clinic.getClosingTime());
    }

    private ClinicDto convertToDto(Clinic clinic) {
        ClinicDto dto = new ClinicDto();
        dto.setId(clinic.getId());
        dto.setName(clinic.getName());
        dto.setAddress(clinic.getAddress());
        dto.setPhoneNumber(clinic.getPhoneNumber());
        dto.setLatitude(clinic.getLatitude());
        dto.setLongitude(clinic.getLongitude());
        dto.setDescription(clinic.getDescription());
        dto.setEmail(clinic.getEmail());
        dto.setOpenNow(clinic.getOpenNow());
        dto.setOpeningTime(clinic.getOpeningTime());
        dto.setClosingTime(clinic.getClosingTime());
        dto.setEmergencyService(clinic.getEmergencyService());
        dto.setAcceptsInsurance(clinic.getAcceptsInsurance());
        dto.setSpecialities(clinic.getSpecialities());
        dto.setImageUrl(clinic.getImageUrl());
        return dto;
    }

    private Clinic convertToEntity(ClinicDto dto) {
        Clinic clinic = new Clinic();
        if (dto.getId() != null) {
            clinic.setId(dto.getId());
        }
        clinic.setName(dto.getName());
        clinic.setAddress(dto.getAddress());
        clinic.setPhoneNumber(dto.getPhoneNumber());
        clinic.setLatitude(dto.getLatitude());
        clinic.setLongitude(dto.getLongitude());
        clinic.setDescription(dto.getDescription());
        clinic.setEmail(dto.getEmail());
        clinic.setOpenNow(dto.getOpenNow() != null ? dto.getOpenNow() : Boolean.TRUE);
        clinic.setOpeningTime(dto.getOpeningTime());
        clinic.setClosingTime(dto.getClosingTime());
        clinic.setEmergencyService(dto.getEmergencyService() != null ? dto.getEmergencyService() : Boolean.FALSE);
        clinic.setAcceptsInsurance(dto.getAcceptsInsurance() != null ? dto.getAcceptsInsurance() : Boolean.FALSE);
        clinic.setSpecialities(dto.getSpecialities());
        clinic.setImageUrl(dto.getImageUrl());
        return clinic;
    }

    private void validateLocationRequest(PharmacyLocationRequest request) {
        if (request.getPatientLatitude() == null || request.getPatientLongitude() == null) {
            throw new IllegalArgumentException("Patient latitude and longitude are required");
        }
        if (request.getRadiusKm() == null || request.getRadiusKm() <= 0) {
            request.setRadiusKm(10);
        }
    }
}
