package com.esprit.microservice.prescriptionservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "careplan-service", path = "/api/care-plans")
public interface CarePlanServiceClient {

    /**
     * Récupère le PDF d'un plan de soins depuis le careplan-service
     */
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    byte[] getCarePlanPdf(@PathVariable("id") Long carePlanId);
}
