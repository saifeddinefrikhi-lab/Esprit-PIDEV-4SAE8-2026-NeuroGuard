package com.neuroguard.pharmacy.repositories;

import com.neuroguard.pharmacy.entities.Clinic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClinicRepository extends JpaRepository<Clinic, Long> {

    List<Clinic> findByNameContainingIgnoreCase(String name);

    List<Clinic> findByEmergencyServiceTrue();

    List<Clinic> findByAcceptsInsuranceTrue();

    @Query(value = "SELECT * FROM clinics WHERE " +
            "(6371 * acos(cos(radians(:patientLat)) * cos(radians(latitude)) * " +
            "cos(radians(longitude) - radians(:patientLon)) + " +
            "sin(radians(:patientLat)) * sin(radians(latitude)))) <= :radiusKm",
            nativeQuery = true)
    List<Clinic> findClinicsWithinRadius(
            @Param("patientLat") Double patientLatitude,
            @Param("patientLon") Double patientLongitude,
            @Param("radiusKm") Integer radiusKm
    );
}
