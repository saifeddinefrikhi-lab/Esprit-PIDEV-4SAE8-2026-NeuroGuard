package com.esprit.microservice.prescriptionservice.services;

import com.esprit.microservice.prescriptionservice.dto.DosageAnalyticsDTO;
import com.esprit.microservice.prescriptionservice.dto.FrequencyAnalyticsDTO;
import com.esprit.microservice.prescriptionservice.dto.PrescriptionAnalyticsDTO;
import com.esprit.microservice.prescriptionservice.dto.SimpleStatsDTO;
import com.esprit.microservice.prescriptionservice.entities.Prescription;
import com.esprit.microservice.prescriptionservice.repositories.PrescriptionRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PrescriptionAnalyticsService {

    private final PrescriptionRepository prescriptionRepository;

    public PrescriptionAnalyticsService(PrescriptionRepository prescriptionRepository) {
        this.prescriptionRepository = prescriptionRepository;
    }

    public PrescriptionAnalyticsDTO getGlobalAnalytics() {
        List<Prescription> allPrescriptions = prescriptionRepository.findAll();
        
        PrescriptionAnalyticsDTO analytics = new PrescriptionAnalyticsDTO();
        
        // Basic metrics
        analytics.setTotalPrescriptions((long) allPrescriptions.size());
        analytics.setTotalPatients(allPrescriptions.stream()
            .map(Prescription::getPatientId)
            .distinct()
            .count());
        analytics.setTotalProviders(allPrescriptions.stream()
            .map(Prescription::getProviderId)
            .distinct()
            .count());
        
        // Dosage Analysis
        List<DosageAnalyticsDTO> dosageAnalysis = analyzeDosages(allPrescriptions);
        analytics.setDosageAnalysis(dosageAnalysis);
        analytics.setHighRiskDosageCount((int) dosageAnalysis.stream()
            .filter(d -> "HIGH".equals(d.getRiskLevel()))
            .count());
        analytics.setTopDosage(dosageAnalysis.stream()
            .max(Comparator.comparingInt(DosageAnalyticsDTO::getCount))
            .map(DosageAnalyticsDTO::getDosage)
            .orElse("N/A"));
        
        // Frequency Analysis
        List<FrequencyAnalyticsDTO> frequencyAnalysis = analyzeFrequencies(allPrescriptions);
        analytics.setFrequencyAnalysis(frequencyAnalysis);
        analytics.setHighComplianceRiskCount((int) frequencyAnalysis.stream()
            .filter(f -> "HIGH".equals(f.getComplianceRisk()))
            .count());
        analytics.setMostCommonFrequency(frequencyAnalysis.stream()
            .max(Comparator.comparingInt(FrequencyAnalyticsDTO::getCount))
            .map(FrequencyAnalyticsDTO::getFrequency)
            .orElse("N/A"));
        
        // Complexity metrics
        analytics.setAverageDosageComplexity(calculateDosageComplexity(allPrescriptions));
        analytics.setAverageFrequencyComplexity(calculateFrequencyComplexity(allPrescriptions));
        analytics.setAveragePrescriptionsPerPatient(
            analytics.getTotalPrescriptions() / (double) Math.max(analytics.getTotalPatients(), 1)
        );
        
        // Risk assessment
        analytics.setPrescriptionsRequiringReview(countPrescriptionsRequiringReview(allPrescriptions));
        analytics.setRecommendations(generateRecommendations(analytics, dosageAnalysis, frequencyAnalysis));
        
        return analytics;
    }

    private List<DosageAnalyticsDTO> analyzeDosages(List<Prescription> prescriptions) {
        Map<String, Long> dosageCounts = prescriptions.stream()
            .filter(p -> p.getDosage() != null && !p.getDosage().isEmpty())
            .collect(Collectors.groupingByConcurrent(
                Prescription::getDosage,
                Collectors.counting()
            ));
        
        long totalWithDosage = dosageCounts.values().stream().mapToLong(Long::longValue).sum();
        
        return dosageCounts.entrySet().stream()
            .map(entry -> {
                DosageAnalyticsDTO dto = new DosageAnalyticsDTO();
                dto.setDosage(entry.getKey());
                dto.setCount(entry.getValue().intValue());
                dto.setPercentage((entry.getValue() / (double) totalWithDosage) * 100);
                dto.setRiskLevel(assessDosageRisk(entry.getKey()));
                dto.setRecommendation(getDosageRecommendation(entry.getKey()));
                return dto;
            })
            .sorted(Comparator.comparingInt(DosageAnalyticsDTO::getCount).reversed())
            .collect(Collectors.toList());
    }

    private List<FrequencyAnalyticsDTO> analyzeFrequencies(List<Prescription> prescriptions) {
        Map<String, Long> frequencyCounts = prescriptions.stream()
            .filter(p -> p.getJour() != null && !p.getJour().isEmpty())
            .collect(Collectors.groupingByConcurrent(
                Prescription::getJour,
                Collectors.counting()
            ));
        
        long totalWithFrequency = frequencyCounts.values().stream().mapToLong(Long::longValue).sum();
        
        return frequencyCounts.entrySet().stream()
            .map(entry -> {
                FrequencyAnalyticsDTO dto = new FrequencyAnalyticsDTO();
                dto.setFrequency(entry.getKey());
                dto.setCount(entry.getValue().intValue());
                dto.setPercentage((entry.getValue() / (double) totalWithFrequency) * 100);
                dto.setTotalDosesPerMonth(calculateDosesPerMonth(entry.getKey()));
                dto.setComplianceRisk(assessComplianceRisk(entry.getKey(), dto.getTotalDosesPerMonth()));
                return dto;
            })
            .sorted(Comparator.comparingInt(FrequencyAnalyticsDTO::getCount).reversed())
            .collect(Collectors.toList());
    }

    private String assessDosageRisk(String dosage) {
        // Simple heuristic: dosages with numbers suggesting high doses
        if (dosage == null || dosage.isEmpty()) return "LOW";
        
        try {
            // Extract numeric values from dosage
            String[] parts = dosage.toLowerCase().split("[^0-9.]+");
            for (String part : parts) {
                if (!part.isEmpty()) {
                    double value = Double.parseDouble(part);
                    if (value > 1000) return "HIGH";
                    if (value > 500) return "MEDIUM";
                }
            }
        } catch (Exception e) {
            // If parsing fails, assess by keywords
            if (dosage.toLowerCase().contains("haute") || dosage.toLowerCase().contains("high")) {
                return "HIGH";
            }
        }
        return "LOW";
    }

    private String getDosageRecommendation(String dosage) {
        String risk = assessDosageRisk(dosage);
        if ("HIGH".equals(risk)) {
            return "Vérifier la dose avec le prestataire - risque d'émission élevé";
        } else if ("MEDIUM".equals(risk)) {
            return "Surveiller la conformité du dosage";
        }
        return "Dosage standard";
    }

    private Integer calculateDosesPerMonth(String frequency) {
        if (frequency == null || frequency.isEmpty()) return 0;
        
        String freq = frequency.toLowerCase();
        if (freq.contains("quotidien") || freq.contains("daily") || freq.contains("tous les jours") || freq.contains("1x")) {
            return 30;
        } else if (freq.contains("2x") || freq.contains("deux fois")) {
            return 60;
        } else if (freq.contains("3x") || freq.contains("trois fois")) {
            return 90;
        } else if (freq.contains("hebdo") || freq.contains("weekly") || freq.contains("semaine")) {
            return 4;
        } else if (freq.contains("mensuel") || freq.contains("monthly")) {
            return 1;
        }
        return 0;
    }

    private String assessComplianceRisk(String frequency, Integer dosesPerMonth) {
        // High frequency complex regimens are harder to comply with
        if (dosesPerMonth != null && dosesPerMonth > 90) {
            return "HIGH";
        } else if (dosesPerMonth != null && dosesPerMonth > 30) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private Double calculateDosageComplexity(List<Prescription> prescriptions) {
        return prescriptions.stream()
            .mapToDouble(p -> {
                String dosage = p.getDosage();
                if (dosage == null || dosage.isEmpty()) return 0;
                // Complexity: number of components in dosage
                return (double) dosage.split("[,+]").length;
            })
            .average()
            .orElse(0.0);
    }

    private Double calculateFrequencyComplexity(List<Prescription> prescriptions) {
        return prescriptions.stream()
            .mapToDouble(p -> {
                String frequency = p.getJour();
                if (frequency == null || frequency.isEmpty()) return 0;
                Integer dosesPerMonth = calculateDosesPerMonth(frequency);
                // Complexity score based on doses per month
                if (dosesPerMonth > 90) return 3.0;
                if (dosesPerMonth > 30) return 2.0;
                if (dosesPerMonth > 4) return 1.5;
                return 1.0;
            })
            .average()
            .orElse(0.0);
    }

    private Integer countPrescriptionsRequiringReview(List<Prescription> prescriptions) {
        return (int) prescriptions.stream()
            .filter(p -> {
                String dosage = p.getDosage();
                String frequency = p.getJour();
                return (dosage != null && "HIGH".equals(assessDosageRisk(dosage))) ||
                       (frequency != null && "HIGH".equals(assessComplianceRisk(frequency, 
                           calculateDosesPerMonth(frequency))));
            })
            .count();
    }

    private List<String> generateRecommendations(PrescriptionAnalyticsDTO analytics,
                                                    List<DosageAnalyticsDTO> dosageAnalysis,
                                                    List<FrequencyAnalyticsDTO> frequencyAnalysis) {
        List<String> recommendations = new ArrayList<>();
        
        if (analytics.getHighRiskDosageCount() > 0) {
            recommendations.add("⚠️ " + analytics.getHighRiskDosageCount() + 
                " prescriptions avec dosages à risque élevé - révision requise");
        }
        
        if (analytics.getHighComplianceRiskCount() > 0) {
            recommendations.add("📋 " + analytics.getHighComplianceRiskCount() + 
                " prescriptions avec risque de conformité élevé - simplifier si possible");
        }
        
        if (analytics.getAveragePrescriptionsPerPatient() > 5) {
            recommendations.add("💊 Charge thérapeutique importante - envisager une consolidation");
        }
        
        if (analytics.getAverageDosageComplexity() > 2) {
            recommendations.add("🔬 Dosages complexes détectés - clarifier les instructions");
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("✅ Les prescriptions sont généralement optimales");
        }
        
        return recommendations;
    }

    // Simple Stats Method - FAST and RELIABLE
    public SimpleStatsDTO getSimpleStats() {
        List<Prescription> allPrescriptions = prescriptionRepository.findAll();
        
        SimpleStatsDTO stats = new SimpleStatsDTO();
        
        // Total prescriptions
        stats.setTotalPrescriptions((long) allPrescriptions.size());
        
        // Count unique doctors
        stats.setTotalDoctors(
            allPrescriptions.stream()
                .map(Prescription::getProviderId)
                .distinct()
                .count()
        );
        
        // Count unique patients
        stats.setTotalPatients(
            allPrescriptions.stream()
                .map(Prescription::getPatientId)
                .distinct()
                .count()
        );
        
        // Count recent prescriptions (last 7 days)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        stats.setRecentPrescriptions(
            allPrescriptions.stream()
                .filter(p -> p.getCreatedAt() != null && 
                       p.getCreatedAt().isAfter(sevenDaysAgo))
                .count()
        );
        
        // Last updated
        stats.setLastUpdated(LocalDateTime.now());
        
        return stats;
    }
}
