package com.neuroguard.pharmacy.repositories;

import com.neuroguard.pharmacy.entities.Pharmacy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {
    
    List<Pharmacy> findAll();
    
    // Find pharmacies that are currently open
    List<Pharmacy> findByOpenNowTrue();
    
    // Find pharmacies with 24-hour service
    List<Pharmacy> findByAccepts24hTrue();
    
    // Find pharmacies with delivery service
    List<Pharmacy> findByHasDeliveryTrue();
    
    // Find pharmacy by name (partial match)
    List<Pharmacy> findByNameContainingIgnoreCase(String name);
    
    // Native query to find pharmacies within a certain radius using distance formula
    @Query(value = "SELECT * FROM pharmacies WHERE " +
           "(6371 * acos(cos(radians(:patientLat)) * cos(radians(latitude)) * " +
           "cos(radians(longitude) - radians(:patientLon)) + " +
           "sin(radians(:patientLat)) * sin(radians(latitude)))) <= :radiusKm", 
           nativeQuery = true)
    List<Pharmacy> findPharmaciesWithinRadius(
        @Param("patientLat") Double patientLatitude,
        @Param("patientLon") Double patientLongitude,
        @Param("radiusKm") Integer radiusKm
    );
}
