package com.esprit.microservice.prescriptionservice.feign;

import com.esprit.microservice.prescriptionservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "user-service", path = "/users")
public interface UserServiceClient {

    @GetMapping("/{id}")
    UserDto getUserById(@PathVariable("id") Long id);

    @GetMapping("/caregiver/{caregiverId}/patients/{patientId}/assigned")
    Boolean isCaregiverAssignedToPatient(@PathVariable("caregiverId") Long caregiverId,
                                         @PathVariable("patientId") Long patientId);

    @GetMapping("/caregiver/{caregiverId}/patients")
    List<Long> getPatientIdsByCaregiver(@PathVariable("caregiverId") Long caregiverId);

    @GetMapping("/patient/{patientId}/caregivers")
    List<Long> getCaregiverIdsByPatient(@PathVariable("patientId") Long patientId);
}
