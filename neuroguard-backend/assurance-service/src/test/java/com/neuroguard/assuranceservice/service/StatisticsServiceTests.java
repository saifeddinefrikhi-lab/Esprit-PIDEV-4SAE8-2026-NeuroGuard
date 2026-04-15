package com.neuroguard.assuranceservice.service;

import com.neuroguard.assuranceservice.dto.StatisticsDto;
import com.neuroguard.assuranceservice.entity.Assurance;
import com.neuroguard.assuranceservice.entity.AssuranceStatus;
import com.neuroguard.assuranceservice.entity.CoverageRiskAssessment;
import com.neuroguard.assuranceservice.repository.AssuranceRepository;
import com.neuroguard.assuranceservice.repository.CoverageRiskAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatisticsService Unit Tests")
class StatisticsServiceTests {

    @Mock
    private AssuranceRepository assuranceRepository;

    @Mock
    private CoverageRiskAssessmentRepository riskAssessmentRepository;

    @InjectMocks
    private StatisticsService statisticsService;

    private Assurance testAssurance;
    private CoverageRiskAssessment testRiskAssessment;

    @BeforeEach
    void setUp() {
        // Initialize test assurance
        testAssurance = new Assurance();
        testAssurance.setId(1L);
        testAssurance.setPatientId(1L);
        testAssurance.setProviderName("Health Insurance Corp");
        testAssurance.setPolicyNumber("POL-12345-67890");
        testAssurance.setCoverageDetails("Comprehensive coverage");
        testAssurance.setIllness("Alzheimer's Disease");
        testAssurance.setPostalCode("75001");
        testAssurance.setMobilePhone("+33612345678");
        testAssurance.setStatus(AssuranceStatus.APPROVED);
        testAssurance.setEstimatedAnnualClaimCost(5000.0);
        testAssurance.setCreatedAt(LocalDateTime.now());
        testAssurance.setUpdatedAt(LocalDateTime.now());

        // Initialize test risk assessment
        testRiskAssessment = new CoverageRiskAssessment();
        testRiskAssessment.setId(1L);
        testRiskAssessment.setAssuranceId(1L);
        testRiskAssessment.setAlzheimersPredictionScore(0.85);
        testRiskAssessment.setEstimatedAnnualClaimCost(5000.0);
        testRiskAssessment.setActiveAlertCount(2);
        testRiskAssessment.setHighestAlertSeverity("WARNING");
        testRiskAssessment.setMedicalComplexityScore(7);
        testRiskAssessment.setRecommendedProviderCount(3);
        testRiskAssessment.setNeurologyReferralNeeded(true);
        testRiskAssessment.setGeriatricAssessmentNeeded(true);

        List<String> procedures = new ArrayList<>();
        procedures.add("CardiacMonitoring");
        procedures.add("CognitiveAssessment");
        testRiskAssessment.setRecommendedProcedures(procedures);

        testAssurance.setCoverageRiskAssessment(testRiskAssessment);
    }

    @Test
    @DisplayName("Should calculate patient statistics successfully")
    void testGetPatientStatistics_Success() {
        // Arrange
        List<Assurance> patientAssurances = new ArrayList<>();
        patientAssurances.add(testAssurance);
        when(assuranceRepository.findByPatientId(1L)).thenReturn(patientAssurances);

        // Act
        StatisticsDto.PatientStatistics stats = statisticsService.getPatientStatistics(1L);

        // Assert
        assertNotNull(stats);
        assertEquals(1L, stats.getPatientId());
        assertEquals(1, stats.getTotalAssurances());
        assertEquals(0.85, stats.getAverageAlzheimersRisk());
        assertEquals(5000.0, stats.getTotalEstimatedCost());
        assertEquals(2, stats.getTotalActiveAlerts());
        verify(assuranceRepository, times(1)).findByPatientId(1L);
    }

    @Test
    @DisplayName("Should return zero statistics when patient has no assurances")
    void testGetPatientStatistics_NoAssurances() {
        // Arrange
        List<Assurance> patientAssurances = new ArrayList<>();
        when(assuranceRepository.findByPatientId(1L)).thenReturn(patientAssurances);

        // Act
        StatisticsDto.PatientStatistics stats = statisticsService.getPatientStatistics(1L);

        // Assert
        assertNotNull(stats);
        assertEquals(1L, stats.getPatientId());
        assertEquals(0, stats.getTotalAssurances());
        verify(assuranceRepository, times(1)).findByPatientId(1L);
    }

    @Test
    @DisplayName("Should return statistics with no risk assessment")
    void testGetPatientStatistics_NoRiskAssessment() {
        // Arrange
        Assurance assuranceWithoutRisk = new Assurance();
        assuranceWithoutRisk.setId(1L);
        assuranceWithoutRisk.setPatientId(1L);
        assuranceWithoutRisk.setProviderName("Insurance Corp");
        assuranceWithoutRisk.setPolicyNumber("POL-123");
        assuranceWithoutRisk.setIllness("Illness");
        assuranceWithoutRisk.setPostalCode("75001");
        assuranceWithoutRisk.setMobilePhone("+33612345678");
        assuranceWithoutRisk.setStatus(AssuranceStatus.PENDING);
        assuranceWithoutRisk.setCoverageRiskAssessment(null);

        List<Assurance> patientAssurances = new ArrayList<>();
        patientAssurances.add(assuranceWithoutRisk);
        when(assuranceRepository.findByPatientId(1L)).thenReturn(patientAssurances);

        // Act
        StatisticsDto.PatientStatistics stats = statisticsService.getPatientStatistics(1L);

        // Assert
        assertNotNull(stats);
        assertEquals(1, stats.getTotalAssurances());
        verify(assuranceRepository, times(1)).findByPatientId(1L);
    }

    @Test
    @DisplayName("Should calculate assurance statistics successfully")
    void testGetAssuranceStatistics_Success() {
        // Arrange
        when(assuranceRepository.findById(1L)).thenReturn(Optional.of(testAssurance));

        // Act
        StatisticsDto.AssuranceStatistics stats = statisticsService.getAssuranceStatistics(1L);

        // Assert
        assertNotNull(stats);
        assertEquals(1L, stats.getAssuranceId());
        assertEquals(0.85, stats.getAverageRiskScore());
        assertEquals(5000.0, stats.getTotalProjectedCost());
        assertEquals(1, stats.getPatientsHighRisk());
        verify(assuranceRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should return empty statistics when assurance not found")
    void testGetAssuranceStatistics_NotFound() {
        // Arrange
        when(assuranceRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        StatisticsDto.AssuranceStatistics stats = statisticsService.getAssuranceStatistics(1L);

        // Assert
        assertNotNull(stats);
        assertEquals(1L, stats.getAssuranceId());
        verify(assuranceRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should calculate correct risk distribution")
    void testGetAssuranceStatistics_RiskDistribution() {
        // Arrange
        CoverageRiskAssessment highRiskAssessment = new CoverageRiskAssessment();
        highRiskAssessment.setId(2L);
        highRiskAssessment.setAssuranceId(1L);
        highRiskAssessment.setAlzheimersPredictionScore(0.85);
        highRiskAssessment.setEstimatedAnnualClaimCost(8000.0);
        highRiskAssessment.setActiveAlertCount(3);
        highRiskAssessment.setHighestAlertSeverity("CRITICAL");
        highRiskAssessment.setMedicalComplexityScore(9);
        highRiskAssessment.setRecommendedProviderCount(4);
        highRiskAssessment.setNeurologyReferralNeeded(true);
        highRiskAssessment.setGeriatricAssessmentNeeded(true);

        testAssurance.setCoverageRiskAssessment(highRiskAssessment);
        when(assuranceRepository.findById(1L)).thenReturn(Optional.of(testAssurance));

        // Act
        StatisticsDto.AssuranceStatistics stats = statisticsService.getAssuranceStatistics(1L);

        // Assert
        assertNotNull(stats);
        assertEquals(0.85, stats.getAverageRiskScore());
        assertEquals(1, stats.getPatientsHighRisk());
        verify(assuranceRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should calculate correct cost metrics")
    void testGetPatientStatistics_CostMetrics() {
        // Arrange
        List<Assurance> assurances = new ArrayList<>();
        
        Assurance assurance1 = new Assurance();
        assurance1.setId(1L);
        assurance1.setPatientId(1L);
        assurance1.setProviderName("Insurance Corp 1");
        assurance1.setPolicyNumber("POL-001");
        assurance1.setIllness("Illness 1");
        assurance1.setPostalCode("75001");
        assurance1.setMobilePhone("+33612345678");
        assurance1.setStatus(AssuranceStatus.APPROVED);

        CoverageRiskAssessment risk1 = new CoverageRiskAssessment();
        risk1.setAlzheimersPredictionScore(0.5);
        risk1.setEstimatedAnnualClaimCost(3000.0);
        risk1.setActiveAlertCount(1);
        risk1.setHighestAlertSeverity("WARNING");
        risk1.setMedicalComplexityScore(5);
        risk1.setRecommendedProviderCount(2);
        risk1.setNeurologyReferralNeeded(false);
        risk1.setGeriatricAssessmentNeeded(false);
        assurance1.setCoverageRiskAssessment(risk1);

        assurances.add(testAssurance);
        assurances.add(assurance1);
        when(assuranceRepository.findByPatientId(1L)).thenReturn(assurances);

        // Act
        StatisticsDto.PatientStatistics stats = statisticsService.getPatientStatistics(1L);

        // Assert
        assertNotNull(stats);
        assertEquals(2, stats.getTotalAssurances());
        assertEquals(8000.0, stats.getTotalEstimatedCost());
        assertTrue(stats.getAverageAnnualCost() > 0);
        verify(assuranceRepository, times(1)).findByPatientId(1L);
    }

    @Test
    @DisplayName("Should calculate complexity score metrics")
    void testGetPatientStatistics_ComplexityMetrics() {
        // Arrange
        List<Assurance> assurances = new ArrayList<>();
        assurances.add(testAssurance);
        when(assuranceRepository.findByPatientId(1L)).thenReturn(assurances);

        // Act
        StatisticsDto.PatientStatistics stats = statisticsService.getPatientStatistics(1L);

        // Assert
        assertNotNull(stats);
        assertTrue(stats.getAverageComplexityScore() > 0);
        assertEquals(7, stats.getMaxComplexityScore());
        verify(assuranceRepository, times(1)).findByPatientId(1L);
    }

    @Test
    @DisplayName("Should identify care team requirements")
    void testGetPatientStatistics_CareTeamAnalysis() {
        // Arrange
        List<Assurance> assurances = new ArrayList<>();
        assurances.add(testAssurance);
        when(assuranceRepository.findByPatientId(1L)).thenReturn(assurances);

        // Act
        StatisticsDto.PatientStatistics stats = statisticsService.getPatientStatistics(1L);

        // Assert
        assertNotNull(stats);
        assertTrue(stats.getPatientsNeedingNeurology() > 0);
        assertTrue(stats.getPatientsNeedingGeriatrics() > 0);
        verify(assuranceRepository, times(1)).findByPatientId(1L);
    }

    @Test
    @DisplayName("Should calculate procedure frequency")
    void testGetPatientStatistics_ProcedureFrequency() {
        // Arrange
        List<Assurance> assurances = new ArrayList<>();
        assurances.add(testAssurance);
        when(assuranceRepository.findByPatientId(1L)).thenReturn(assurances);

        // Act
        StatisticsDto.PatientStatistics stats = statisticsService.getPatientStatistics(1L);

        // Assert
        assertNotNull(stats);
        assertNotNull(stats.getRecommendedProceduresFrequency());
        assertTrue(stats.getRecommendedProceduresFrequency().size() > 0);
        verify(assuranceRepository, times(1)).findByPatientId(1L);
    }

    @Test
    @DisplayName("Should determine risk level based on Alzheimer score")
    void testGetPatientStatistics_RiskLevelAssessment() {
        // Arrange
        List<Assurance> assurances = new ArrayList<>();
        assurances.add(testAssurance);
        when(assuranceRepository.findByPatientId(1L)).thenReturn(assurances);

        // Act
        StatisticsDto.PatientStatistics stats = statisticsService.getPatientStatistics(1L);

        // Assert
        assertNotNull(stats);
        assertNotNull(stats.getOverallRiskLevel());
        verify(assuranceRepository, times(1)).findByPatientId(1L);
    }
}
