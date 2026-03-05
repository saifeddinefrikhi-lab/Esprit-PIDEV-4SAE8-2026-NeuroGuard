package com.neuroguard.riskalertservice.client;

import com.neuroguard.riskalertservice.dto.PatientFeatures;
import com.neuroguard.riskalertservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "medical-history-service", path = "/api/provider/medical-history")
public interface MedicalHistoryProviderClient {

    @GetMapping("/features/{patientId}")
    PatientFeatures getPatientFeatures(@PathVariable("patientId") Long patientId);

    @GetMapping("/patients")
    List<UserDto> getAssignedPatientsForProvider();  // will be called with provider ID in header
}