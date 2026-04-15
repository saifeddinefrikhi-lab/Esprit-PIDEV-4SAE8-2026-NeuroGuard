package com.esprit.microservice.careplanservice.controllers;

import com.esprit.microservice.careplanservice.dto.CarePlanMessageRequest;
import com.esprit.microservice.careplanservice.dto.CarePlanMessageResponse;
import com.esprit.microservice.careplanservice.dto.CarePlanRequest;
import com.esprit.microservice.careplanservice.dto.CarePlanResponse;
import com.esprit.microservice.careplanservice.dto.CarePlanStatsResponse;
import com.esprit.microservice.careplanservice.dto.StatusUpdateRequest;
import com.esprit.microservice.careplanservice.dto.CarePlanStatisticsDto;
import com.esprit.microservice.careplanservice.entities.CarePlan;
import com.esprit.microservice.careplanservice.services.CarePlanService;
import com.esprit.microservice.careplanservice.services.StatisticsService;
import com.esprit.microservice.careplanservice.services.PdfGeneratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/care-plans")
@RequiredArgsConstructor
public class CarePlanController {

    private final CarePlanService carePlanService;
    private final StatisticsService statisticsService;
    private final PdfGeneratorService pdfGeneratorService;

    @PostMapping
    public ResponseEntity<CarePlanResponse> createCarePlan(@Valid @RequestBody CarePlanRequest request) {
        CarePlanResponse response = carePlanService.createCarePlan(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CarePlanResponse> updateCarePlan(@PathVariable Long id,
                                                           @Valid @RequestBody CarePlanRequest request) {
        CarePlanResponse response = carePlanService.updateCarePlan(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/list")
    public ResponseEntity<List<CarePlanResponse>> getCarePlansList() {
        List<CarePlanResponse> responses = carePlanService.getCarePlansList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/stats")
    public ResponseEntity<CarePlanStatsResponse> getStats() {
        CarePlanStatsResponse response = carePlanService.getStats();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistics/detailed")
    public ResponseEntity<List<CarePlanStatisticsDto>> getDetailedStatistics() {
        List<CarePlanStatisticsDto> stats = statisticsService.getCarePlanStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/statistics/by-patient")
    public ResponseEntity<List<Object[]>> getCarePlansPerPatient() {
        List<Object[]> data = statisticsService.getCarePlansPerPatient();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/statistics/by-provider")
    public ResponseEntity<List<Object[]>> getCarePlansPerProvider() {
        List<Object[]> data = statisticsService.getCarePlansPerProvider();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/statistics/count/patient/{patientId}")
    public ResponseEntity<Long> getCarePlanCountByPatient(@PathVariable Long patientId) {
        Long count = statisticsService.getCarePlanCountByPatient(patientId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/statistics/count/provider/{providerId}")
    public ResponseEntity<Long> getCarePlanCountByProvider(@PathVariable Long providerId) {
        Long count = statisticsService.getCarePlanCountByProvider(providerId);
        return ResponseEntity.ok(count);
    }

    /** Generate PDF for a specific care plan - MUST be before /{id} */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> generateCarePlanPdf(@PathVariable Long id) {
        try {
            CarePlan carePlan = carePlanService.getCarePlanEntityById(id);
            byte[] pdfBytes = pdfGeneratorService.generateCarePlanPdf(carePlan);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "care-plan-" + id + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCarePlan(@PathVariable Long id) {
        carePlanService.deleteCarePlan(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CarePlanResponse> getCarePlanById(@PathVariable Long id) {
        CarePlanResponse response = carePlanService.getCarePlanById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<CarePlanResponse>> getCarePlansByPatient(@RequestParam Long patientId) {
        List<CarePlanResponse> responses = carePlanService.getCarePlansByPatient(patientId);
        return ResponseEntity.ok(responses);
    }

    /** Patient only: set one section's status (nutrition, sleep, activity, medication) to TODO or DONE. */
    @PatchMapping("/{id}/status")
    public ResponseEntity<CarePlanResponse> updateSectionStatus(@PathVariable Long id,
                                                               @RequestBody StatusUpdateRequest request) {
        String section = request != null ? request.getSection() : null;
        String status = request != null ? request.getStatus() : null;
        CarePlanResponse response = carePlanService.updateSectionStatus(id, section, status);
        return ResponseEntity.ok(response);
    }

    /** Get chat messages between doctor and patient for this care plan. */
    @GetMapping("/{id}/messages")
    public ResponseEntity<List<CarePlanMessageResponse>> getMessages(@PathVariable Long id) {
        List<CarePlanMessageResponse> messages = carePlanService.getMessages(id);
        return ResponseEntity.ok(messages);
    }

    /** Send a message (doctor or patient only). */
    @PostMapping("/{id}/messages")
    public ResponseEntity<CarePlanMessageResponse> sendMessage(@PathVariable Long id,
                                                              @Valid @RequestBody CarePlanMessageRequest request) {
        CarePlanMessageResponse response = carePlanService.sendMessage(id, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


}