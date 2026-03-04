package com.neuroguard.riskalertservice.repository;

import com.neuroguard.riskalertservice.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByPatientId(Long patientId);

    @Query("SELECT a FROM Alert a WHERE a.patientId IN :patientIds")
    List<Alert> findByPatientIdIn(@Param("patientIds") List<Long> patientIds);

    boolean existsByPatientIdAndMessageAndResolvedFalse(Long patientId, String message);
}