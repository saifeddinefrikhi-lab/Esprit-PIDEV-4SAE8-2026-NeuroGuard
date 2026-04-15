package com.esprit.microservice.careplanservice.services;

import com.esprit.microservice.careplanservice.dto.PrescriptionRiskScoreDto;
import com.esprit.microservice.careplanservice.dto.RiskAnalysisReportDto;
import com.esprit.microservice.careplanservice.entities.Prescription;
import com.esprit.microservice.careplanservice.repositories.PrescriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskAnalysisServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @InjectMocks
    private RiskAnalysisService riskAnalysisService;

    @Test
    void analyzeRisk_withHighContent_returnsHighAndCritical() {
        Prescription p = new Prescription();
        p.setId(1L);
        p.setPatientId(99L);
        p.setContenu("high dose interaction allergy chronic treatment details");

        when(prescriptionRepository.findAll()).thenReturn(List.of(p));

        RiskAnalysisReportDto report = riskAnalysisService.analyzeRisk(null);

        assertEquals(1L, report.getHighRiskCount());
        assertEquals(0L, report.getMediumRiskCount());
        assertEquals(0L, report.getLowRiskCount());
        assertEquals("INCREASING", report.getRiskTrend());
        assertEquals(1, report.getPrescriptionRisks().size());

        PrescriptionRiskScoreDto score = report.getPrescriptionRisks().get(0);
        assertEquals("HIGH", score.getRiskLevel());
        assertTrue(score.getRiskScore() >= 80.0);
    }

    @Test
    void analyzeRisk_withNoPrescriptions_returnsEmptySummary() {
        when(prescriptionRepository.findAll()).thenReturn(List.of());

        RiskAnalysisReportDto report = riskAnalysisService.analyzeRisk(null);

        assertEquals(0L, report.getHighRiskCount());
        assertEquals(0L, report.getMediumRiskCount());
        assertEquals(0L, report.getLowRiskCount());
        assertEquals(0.0, report.getAverageRiskScore());
        assertEquals("DECREASING", report.getRiskTrend());
        assertTrue(report.getOverallHealthAssessment().contains("Aucune prescription"));
    }

    @Test
    void getRiskScoreForPrescription_notFound_throwsException() {
        when(prescriptionRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> riskAnalysisService.getRiskScoreForPrescription(404L));
    }
}
