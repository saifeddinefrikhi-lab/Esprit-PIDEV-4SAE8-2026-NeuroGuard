package com.neuroguard.assuranceservice.client;

import com.neuroguard.assuranceservice.dto.ConsultationResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "consultation-service", url = "${application.config.consultation-url:http://localhost:8081}")
public interface ConsultationClient {

    @GetMapping("/api/consultations/all/patient/{patientId}")
    List<ConsultationResponseDto> getConsultationsByPatient(@PathVariable("patientId") Long patientId);
}
