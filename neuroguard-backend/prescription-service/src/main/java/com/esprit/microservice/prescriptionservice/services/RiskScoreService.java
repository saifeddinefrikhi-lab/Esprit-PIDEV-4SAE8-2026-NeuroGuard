package com.esprit.microservice.prescriptionservice.services;

import com.esprit.microservice.prescriptionservice.dto.PrescriptionRiskScoreDTO;
import com.esprit.microservice.prescriptionservice.dto.RiskAnalysisReportDTO;
import com.esprit.microservice.prescriptionservice.entities.Prescription;
import com.esprit.microservice.prescriptionservice.repositories.PrescriptionRepository;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RiskScoreService {

    private final PrescriptionRepository prescriptionRepository;

    public RiskScoreService(PrescriptionRepository prescriptionRepository) {
        this.prescriptionRepository = prescriptionRepository;
    }

    /**
     * Calculate overall risk analysis for all prescriptions
     */
    public RiskAnalysisReportDTO generateRiskAnalysisReport(Long patientId) {
        List<Prescription> allPrescriptions;
        if (patientId != null) {
            allPrescriptions = prescriptionRepository.findByPatientId(patientId);
        } else {
            allPrescriptions = prescriptionRepository.findAll();
        }
        
        // Calculate individual risk scores
        List<PrescriptionRiskScoreDTO> riskScores = allPrescriptions.stream()
            .map(this::calculatePrescriptionRiskScore)
            .collect(Collectors.toList());
        
        // Create report
        RiskAnalysisReportDTO report = new RiskAnalysisReportDTO();
        
        // Count by risk level
        report.setTotalAnalyzed(riskScores.size());
        report.setCriticalCount((int) riskScores.stream()
            .filter(r -> r.getUrgencyLevel().equals("CRITICAL")).count());
        report.setHighRiskCount((int) riskScores.stream()
            .filter(r -> r.getRiskLevel().equals("HIGH")).count());
        report.setMediumRiskCount((int) riskScores.stream()
            .filter(r -> r.getRiskLevel().equals("MEDIUM")).count());
        report.setLowRiskCount((int) riskScores.stream()
            .filter(r -> r.getRiskLevel().equals("LOW")).count());
        
        // Calculate statistics
        if (!riskScores.isEmpty()) {
            double avgScore = riskScores.stream()
                .mapToInt(PrescriptionRiskScoreDTO::getOverallRiskScore)
                .average()
                .orElse(0);
            report.setAverageRiskScore(avgScore);
            
            report.setMaxRiskScore(riskScores.stream()
                .mapToInt(PrescriptionRiskScoreDTO::getOverallRiskScore)
                .max()
                .orElse(0));
            
            report.setMinRiskScore(riskScores.stream()
                .mapToInt(PrescriptionRiskScoreDTO::getOverallRiskScore)
                .min()
                .orElse(0));
        }
        
        // Add percentile rankings
        assignPercentileRanks(riskScores);
        
        // Set prescriptions
        report.setPrescriptionScores(riskScores.stream()
            .sorted(Comparator.comparingInt(PrescriptionRiskScoreDTO::getOverallRiskScore).reversed())
            .limit(20) // Top 20 highest risk
            .collect(Collectors.toList()));
        
        // Generate system recommendations
        report.setSystemRecommendations(generateSystemRecommendations(riskScores));
        
        // Overall assessment
        report.setOverallHealthAssessment(generateHealthAssessment(report));
        
        // Risk trend
        report.setRiskTrend(calculateRiskTrend(riskScores));
        
        // Top concerns
        report.setTopConcerns(extractTopConcerns(riskScores));
        
        return report;
    }

    /**
     * Calculate risk score for a single prescription
     */
    public PrescriptionRiskScoreDTO calculatePrescriptionRiskScore(Prescription prescription) {
        PrescriptionRiskScoreDTO score = new PrescriptionRiskScoreDTO();
        
        score.setPrescriptionId(prescription.getId());
        score.setDosage(prescription.getDosage());
        score.setFrequency(prescription.getJour());
        
        // Calculate component scores (each 0-25)
        int dosageScore = calculateDosageRisk(prescription.getDosage());
        int frequencyScore = calculateFrequencyRisk(prescription.getJour());
        int complexityScore = calculateComplexityRisk(prescription);
        int patternScore = calculatePatternRisk(prescription);
        
        score.setDosageRiskScore(dosageScore);
        score.setFrequencyRiskScore(frequencyScore);
        score.setComplexityRiskScore(complexityScore);
        score.setPatternRiskScore(patternScore);
        
        // Calculate overall score (0-100)
        int overallScore = dosageScore + frequencyScore + complexityScore + patternScore;
        score.setOverallRiskScore(Math.min(overallScore, 100));
        
        // Determine risk level with more realistic thresholds
        if (overallScore >= 60) {
            score.setRiskLevel("HIGH");
            score.setUrgencyLevel("CRITICAL");
            score.setRequiresImmediateReview(true);
        } else if (overallScore >= 35) {
            score.setRiskLevel("MEDIUM");
            score.setUrgencyLevel("HIGH");
            score.setRequiresImmediateReview(false);
        } else {
            score.setRiskLevel("LOW");
            score.setUrgencyLevel("LOW");
            score.setRequiresImmediateReview(false);
        }
        
        // Generate recommendations
        score.setPrimaryRecommendation(generatePrimaryRecommendation(score));
        score.setSecondaryRecommendation(generateSecondaryRecommendation(score));
        
        return score;
    }

    /**
     * Calculate dosage-based risk (0-25)
     */
    private int calculateDosageRisk(String dosage) {
        if (dosage == null || dosage.isEmpty()) {
            return 18; // Unknown dosage is moderately risky
        }
        
        String lowered = dosage.toLowerCase();
        try {
            String[] parts = lowered.split("[^0-9.]+");
            for (String part : parts) {
                if (!part.isEmpty()) {
                    double value = Double.parseDouble(part);
                    
                    // Risk increases with higher or unusually low dosages
                    if (value >= 2000) return 25;
                    if (value >= 1000) return 23;
                    if (value >= 500) return 20;
                    if (value >= 200) return 15;
                    if (value >= 100) return 10;
                    if (value >= 50) return 8;
                    if (value >= 10) return 6;
                    if (value < 1) return 15;
                    return 12;
                }
            }
        } catch (Exception ignored) {
            // Check for qualitative dosage warnings
            if (lowered.contains("haute") || lowered.contains("high") || lowered.contains("fort")) {
                return 20;
            }
            if (lowered.contains("faible") || lowered.contains("low")) {
                return 12;
            }
        }
        
        return 10; // Standard dosage
    }

    /**
     * Calculate frequency-based risk (0-25)
     */
    private int calculateFrequencyRisk(String frequency) {
        if (frequency == null || frequency.isEmpty()) {
            return 18; // Unknown frequency is moderately risky
        }
        
        String lowered = frequency.toLowerCase();
        
        if (lowered.contains("chaque heure") || lowered.contains("hourly") || lowered.contains("hourly")) {
            return 25;
        }
        if (lowered.contains("4 fois") || lowered.contains("4 times") || lowered.contains("quatre")) {
            return 20;
        }
        if (lowered.contains("3 fois") || lowered.contains("3 times") || lowered.contains("trois")) {
            return 17;
        }
        if (lowered.contains("2 fois") || lowered.contains("2 times") || lowered.contains("twice") || lowered.contains("deux")) {
            return 13;
        }
        if (lowered.contains("1 fois") || lowered.contains("once") || lowered.contains("daily") || lowered.contains("quotidien")) {
            return 9;
        }
        if (lowered.contains("semaine") || lowered.contains("week")) {
            return 6;
        }
        
        return 12; // Ambiguous frequency still counts as moderate risk
    }

    /**
     * Calculate complexity-based risk (0-25)
     */
    private int calculateComplexityRisk(Prescription prescription) {
        int score = 0;
        
        if (prescription.getContenu() != null && !prescription.getContenu().isEmpty()) {
            int commaCount = prescription.getContenu().split(",").length;
            score = Math.min(commaCount * 5, 20);
            if (prescription.getContenu().length() > 80) {
                score += 5;
            }
            String contenuLower = prescription.getContenu().toLowerCase();
            if (contenuLower.contains("interaction") || contenuLower.contains("complex") || contenuLower.contains("association")) {
                score += 5;
            }
        }
        
        if (prescription.getNotes() != null && !prescription.getNotes().isEmpty()) {
            score += 8;
        }
        
        String combined = (prescription.getDosage() != null ? prescription.getDosage() : "") +
                         (prescription.getJour() != null ? prescription.getJour() : "");
        if (combined.length() > 30) {
            score += 5;
        }
        
        return Math.min(score, 25);
    }

    /**
     * Calculate pattern-based risk (0-25) - based on similar prescriptions
     */
    private int calculatePatternRisk(Prescription prescription) {
        List<Prescription> allPrescriptions = prescriptionRepository.findAll();
        long similarCount = allPrescriptions.stream()
            .filter(p -> p.getPatientId().equals(prescription.getPatientId()))
            .filter(p -> p.getDosage() != null && p.getDosage().equals(prescription.getDosage()))
            .count();
        
        if (similarCount > 15) return 25;
        if (similarCount > 8) return 20;
        if (similarCount > 3) return 15;
        if (similarCount > 1) return 10;
        return 5;
    }

    /**
     * Generate primary recommendation
     */
    private String generatePrimaryRecommendation(PrescriptionRiskScoreDTO score) {
        if ("HIGH".equals(score.getRiskLevel())) {
            return "🔴 URGENT: Révision immédiate requise. Consulter le prestataire de santé.";
        }
        if ("MEDIUM".equals(score.getRiskLevel())) {
            return "🟡 Surveiller de près. Évaluer les résultats dans 2-3 jours.";
        }
        return "🟢 Continuer le suivi Standard. Réévalu en routine.";
    }

    /**
     * Generate secondary recommendation
     */
    private String generateSecondaryRecommendation(PrescriptionRiskScoreDTO score) {
        StringBuilder sb = new StringBuilder();
        
        if (score.getDosageRiskScore() > 15) {
            sb.append("• Valider le dosage | ");
        }
        if (score.getFrequencyRiskScore() > 15) {
            sb.append("• Simplifier la fréquence | ");
        }
        if (score.getComplexityRiskScore() > 15) {
            sb.append("• Clarifier les instructions | ");
        }
        if (score.getPatternRiskScore() > 15) {
            sb.append("• Analyser les interactions");
        }
        
        String result = sb.toString();
        return result.isEmpty() ? "Continuer le suivi habituel" : result.replaceAll(" \\| $", "");
    }

    /**
     * Generate system recommendations
     */
    private List<String> generateSystemRecommendations(List<PrescriptionRiskScoreDTO> scores) {
        List<String> recommendations = new ArrayList<>();
        
        long criticalCount = scores.stream()
            .filter(s -> s.getUrgencyLevel().equals("CRITICAL")).count();
        
        if (criticalCount > 0) {
            recommendations.add("⚠️ " + criticalCount + " prescriptions critiques détectées - action immédiate requise");
        }
        
        long highCount = scores.stream()
            .filter(s -> "HIGH".equals(s.getRiskLevel())).count();
        
        if (highCount > 5) {
            recommendations.add("📋 Charge de risque élevée - envisager un audit complet du portefeuille");
        }
        
        double avgScore = scores.stream()
            .mapToInt(PrescriptionRiskScoreDTO::getOverallRiskScore)
            .average()
            .orElse(0);
        
        if (avgScore > 50) {
            recommendations.add("🔍 Score moyen élevé - réviser les protocoles de prescription");
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("✅ Situation généralement contrôlée. Continuer la surveillance habituelle.");
        }
        
        return recommendations;
    }

    /**
     * Generate overall health assessment
     */
    private String generateHealthAssessment(RiskAnalysisReportDTO report) {
        int totalCritical = report.getCriticalCount() + report.getHighRiskCount();
        int total = report.getTotalAnalyzed();
        
        if (total == 0) return "Aucune prescription à analyser";
        
        double riskPercentage = (totalCritical / (double) total) * 100;
        
        if (riskPercentage > 40) {
            return "🔴 CRITIQUE: Plus de 40% des prescriptions présentent des risques importants";
        }
        if (riskPercentage > 20) {
            return "🟡 ATTENTION: Environ " + (int)riskPercentage + "% des prescriptions nécessitent une révision";
        }
        return "🟢 SATISFAISANT: Profil de risque globalement acceptable";
    }

    /**
     * Calculate risk trend
     */
    private String calculateRiskTrend(List<PrescriptionRiskScoreDTO> scores) {
        // Simplified: based on current average
        double avg = scores.stream()
            .mapToInt(PrescriptionRiskScoreDTO::getOverallRiskScore)
            .average()
            .orElse(0);
        
        if (avg > 60) return "INCREASING";
        if (avg > 40) return "STABLE";
        return "DECREASING";
    }

    /**
     * Extract top concerns
     */
    private List<String> extractTopConcerns(List<PrescriptionRiskScoreDTO> scores) {
        return scores.stream()
            .filter(s -> s.getUrgencyLevel().equals("CRITICAL") || s.getUrgencyLevel().equals("HIGH"))
            .sorted(Comparator.comparingInt(PrescriptionRiskScoreDTO::getOverallRiskScore).reversed())
            .limit(5)
            .map(s -> "Rx #" + s.getPrescriptionId() + ": " + s.getPrimaryRecommendation())
            .collect(Collectors.toList());
    }

    /**
     * Assign percentile ranks to scores
     */
    private void assignPercentileRanks(List<PrescriptionRiskScoreDTO> scores) {
        if (scores.isEmpty()) return;
        
        List<Integer> sortedScores = scores.stream()
            .map(PrescriptionRiskScoreDTO::getOverallRiskScore)
            .sorted()
            .collect(Collectors.toList());
        
        for (PrescriptionRiskScoreDTO score : scores) {
            int rank = 0;
            for (Integer s : sortedScores) {
                if (s <= score.getOverallRiskScore()) {
                    rank++;
                }
            }
            score.setPercentileRank((int) ((rank / (double) sortedScores.size()) * 100));
        }
    }
}
