package com.esprit.microservice.prescriptionservice.controllers;

import com.esprit.microservice.prescriptionservice.dto.PrescriptionRequest;
import com.esprit.microservice.prescriptionservice.dto.PrescriptionResponse;
import com.esprit.microservice.prescriptionservice.entities.Prescription;
import com.esprit.microservice.prescriptionservice.services.PrescriptionService;
import com.esprit.microservice.prescriptionservice.services.PdfGeneratorService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
@Slf4j
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final PdfGeneratorService pdfGeneratorService;

    /** Doctor: create prescription */
    @PostMapping
    public ResponseEntity<PrescriptionResponse> createPrescription(@Valid @RequestBody PrescriptionRequest request) {
        PrescriptionResponse response = prescriptionService.createPrescription(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /** Doctor: update prescription */
    @PutMapping("/{id}")
    public ResponseEntity<PrescriptionResponse> updatePrescription(@PathVariable Long id,
                                                                   @Valid @RequestBody PrescriptionRequest request) {
        PrescriptionResponse response = prescriptionService.updatePrescription(id, request);
        return ResponseEntity.ok(response);
    }

    /** Doctor: delete prescription */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrescription(@PathVariable Long id) {
        prescriptionService.deletePrescription(id);
        return ResponseEntity.noContent().build();
    }

    /** List prescriptions for current user (by role: doctor=his, admin=all, patient=his, caregiver=assigned) */
    @GetMapping("/list")
    public ResponseEntity<List<PrescriptionResponse>> getPrescriptionsList() {
        List<PrescriptionResponse> responses = prescriptionService.getPrescriptionsList();
        return ResponseEntity.ok(responses);
    }

    /** Search prescriptions by keyword */
    @GetMapping("/search")
    public ResponseEntity<List<PrescriptionResponse>> searchPrescriptions(@RequestParam String keyword) {
        List<PrescriptionResponse> responses = prescriptionService.searchPrescriptions(keyword);
        return ResponseEntity.ok(responses);
    }

    /** Generate PDF for a specific prescription - MUST be before /{id} */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> generatePrescriptionPdf(@PathVariable Long id) {
        try {
            Prescription prescription = prescriptionService.getPrescriptionEntityById(id);
            byte[] pdfBytes = pdfGeneratorService.generatePrescriptionPdf(prescription);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "prescription-" + id + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /** Generate combined PDF (Care Plan + Prescription) for a specific prescription with its associated care plan */
    @GetMapping("/{prescriptionId}/combined-pdf/{carePlanId}")
    public ResponseEntity<byte[]> generateCombinedPdf(@PathVariable Long prescriptionId, 
                                                      @PathVariable Long carePlanId) {
        try {
            Prescription prescription = prescriptionService.getPrescriptionEntityById(prescriptionId);
            
            // Récupérer le PDF du plan de soins via le service
            byte[] carePlanPdf = null;
            try {
                carePlanPdf = prescriptionService.getCarePlanPdf(carePlanId);
            } catch (Exception e) {
                // Si le plan de soins n'est pas disponible, continuer avec seulement la prescription
                log.warn("Care plan PDF not available for ID: {}", carePlanId);
            }
            
            byte[] pdfBytes = pdfGeneratorService.generateCombinedPdf(carePlanPdf, prescription);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", 
                "medical-document-" + prescriptionId + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /** Patient (read only): get prescription by ID */
    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionResponse> getPrescriptionById(@PathVariable Long id) {
        PrescriptionResponse response = prescriptionService.getPrescriptionById(id);
        return ResponseEntity.ok(response);
    }

}
