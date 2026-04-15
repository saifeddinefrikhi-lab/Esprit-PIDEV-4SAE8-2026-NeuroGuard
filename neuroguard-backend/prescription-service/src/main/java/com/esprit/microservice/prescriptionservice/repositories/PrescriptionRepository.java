package com.esprit.microservice.prescriptionservice.repositories;

import com.esprit.microservice.prescriptionservice.entities.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByPatientId(Long patientId);
    List<Prescription> findByProviderId(Long providerId);

    // Statistiques avec jointures
    @Query("SELECT COUNT(p) FROM Prescription p WHERE p.patientId = :patientId")
    Long countPrescriptionsByPatient(@Param("patientId") Long patientId);

    @Query("SELECT COUNT(p) FROM Prescription p WHERE p.providerId = :providerId")
    Long countPrescriptionsByProvider(@Param("providerId") Long providerId);

    @Query("SELECT CAST(p.createdAt as date) as date, COUNT(p) as count FROM Prescription p GROUP BY CAST(p.createdAt as date) ORDER BY date DESC")
    List<Object[]> getPrescriptionsByDate();

    @Query("SELECT p.patientId, COUNT(p) as count FROM Prescription p GROUP BY p.patientId ORDER BY count DESC")
    List<Object[]> getPrescriptionsPerPatient();

    @Query("SELECT p.providerId, COUNT(p) as count FROM Prescription p GROUP BY p.providerId ORDER BY count DESC")
    List<Object[]> getPrescriptionsPerProvider();

    @Query("SELECT COUNT(DISTINCT p.patientId) FROM Prescription p")
    Long countUniquePatientsWithPrescriptions();

    @Query("SELECT COUNT(p) FROM Prescription p")
    Long countTotalPrescriptions();

    @Query("SELECT YEAR(p.createdAt) as year, MONTH(p.createdAt) as month, COUNT(p) as count FROM Prescription p GROUP BY YEAR(p.createdAt), MONTH(p.createdAt) ORDER BY year DESC, month DESC")
    List<Object[]> getPrescriptionsByMonth();
}
