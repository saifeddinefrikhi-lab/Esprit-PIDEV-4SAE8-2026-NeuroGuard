package com.neuroguard.riskalertservice.client;

import com.neuroguard.riskalertservice.dto.MedicalHistorySummary;
import com.neuroguard.riskalertservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "medical-history-service", path = "/api/caregiver/medical-history")
public interface MedicalHistoryClient {

    @GetMapping("/{patientId}")
    MedicalHistorySummary getMedicalHistoryByPatientId(@PathVariable("patientId") Long patientId);

    @GetMapping("/patients")
    List<UserDto> getAssignedPatientsForCaregiver();  // will be called with caregiver ID in header
}