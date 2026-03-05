package com.neuroguard.consultationservice.repository;

import com.neuroguard.consultationservice.entity.Consultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    List<Consultation> findByProviderId(Long providerId);
    List<Consultation> findByPatientId(Long patientId);
    List<Consultation> findByCaregiverId(Long caregiverId);

    @Query("SELECT c FROM Consultation c WHERE c.providerId = :providerId AND c.status != 'CANCELLED' " +
           "AND c.startTime < :endTime AND (c.endTime IS NULL OR c.endTime > :startTime)")
    List<Consultation> findOverlappingConsultations(@Param("providerId") Long providerId,
                                                    @Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime);
}