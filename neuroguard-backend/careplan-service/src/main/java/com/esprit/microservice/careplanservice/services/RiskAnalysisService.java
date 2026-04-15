package com.esprit.microservice.careplanservice.services;

import com.esprit.microservice.careplanservice.dto.PrescriptionRiskScoreDto;
import com.esprit.microservice.careplanservice.dto.RiskAnalysisReportDto;
import com.esprit.microservice.careplanservice.entities.Prescription;
import com.esprit.microservice.careplanservice.repositories.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RiskAnalysisService {

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    /**
     * Analyse les risques pour toutes les prescriptions ou pour un patient spécifique
     */
    public RiskAnalysisReportDto analyzeRisk(Long patientId) {
        List<Prescription> prescriptions;
        
        if (patientId != null) {
            prescriptions = prescriptionRepository.findByPatientId(patientId);
        } else {
            prescriptions = prescriptionRepository.findAll();
        }

        // Analyser chaque prescription
        List<PrescriptionRiskScoreDto> risks = new ArrayList<>();
        long highRiskCount = 0;
        long mediumRiskCount = 0;
        long lowRiskCount = 0;
        double totalScore = 0;
        int criticalCount = 0;

        for (Prescription prescription : prescriptions) {
            PrescriptionRiskScoreDto riskScore = calculatePrescriptionRisk(prescription);
            risks.add(riskScore);
            totalScore += riskScore.getRiskScore();

            if ("HIGH".equals(riskScore.getRiskLevel())) {
                highRiskCount++;
                if (riskScore.getRiskScore() > 80) criticalCount++;
            } else if ("MEDIUM".equals(riskScore.getRiskLevel())) {
                mediumRiskCount++;
            } else {
                lowRiskCount++;
            }
        }

        double averageScore = !prescriptions.isEmpty() ? totalScore / prescriptions.size() : 0;
        String riskTrend = assessRiskTrend(averageScore);
        String assessment = generateHealthAssessment(highRiskCount, mediumRiskCount, lowRiskCount, prescriptions.size());
        List<String> recommendations = generateRecommendations(highRiskCount, mediumRiskCount, criticalCount);

        return RiskAnalysisReportDto.builder()
                .highRiskCount(highRiskCount)
                .mediumRiskCount(mediumRiskCount)
                .lowRiskCount(lowRiskCount)
                .averageRiskScore(Math.round(averageScore * 100.0) / 100.0)
                .criticalCount(criticalCount)
                .riskTrend(riskTrend)
                .overallHealthAssessment(assessment)
                .systemRecommendations(recommendations)
                .prescriptionRisks(risks)
                .build();
    }

    /**
     * Calcule le score de risque pour une prescription spécifique
     */
    private PrescriptionRiskScoreDto calculatePrescriptionRisk(Prescription prescription) {
        double baseScore = 30;
        String riskLevel = "LOW";
        StringBuilder interactionDetails = new StringBuilder();

        // Analyse simple basée sur la longueur du contenu (en pratique, c'est beaucoup plus complexe)
        int contentLength = prescription.getContenu() != null ? prescription.getContenu().length() : 0;
        double score = baseScore + (contentLength / 100.0);

        // Vérifier les patterns courants de risque
        String content = prescription.getContenu().toLowerCase();
        
        // Augmenter pour certains mots clés
        if (content.contains("hautdose") || content.contains("high dose")) {
            score += 15;
            interactionDetails.append("Dose élevée détectée. ");
        }
        if (content.contains("interaction") || content.contains("contraindication")) {
            score += 20;
            interactionDetails.append("Interaction/contraindication mentionnée. ");
        }
        if (content.contains("allergie") || content.contains("allergy")) {
            score += 25;
            interactionDetails.append("Allergie détectée. ");
        }
        if (content.contains("chronique") || content.contains("chronic")) {
            score += 10;
            interactionDetails.append("Traitement chronique. ");
        }

        // Limiter le score à 100
        score = Math.min(100, score);

        // Déterminer le niveau de risque
        if (score >= 70) {
            riskLevel = "HIGH";
        } else if (score >= 40) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "LOW";
        }

        String recommendation = generateRecommendationForPrescription(riskLevel, score);

        return PrescriptionRiskScoreDto.builder()
                .prescriptionId(prescription.getId())
                .patientId(prescription.getPatientId())
                .riskScore(Math.round(score * 100.0) / 100.0)
                .riskLevel(riskLevel)
                .recommendation(recommendation)
                .interactionDetails(interactionDetails.toString().isEmpty() ? "Pas de détails spécifiques" : interactionDetails.toString())
                .build();
    }

    /**
     * Génère une recommandation basée sur le niveau de risque
     */
    private String generateRecommendationForPrescription(String riskLevel, double score) {
        if ("HIGH".equals(riskLevel)) {
            if (score >= 80) {
                return "⚠️ Critique: Révision immédiate requise. Consultation du pharmacien recommandée.";
            }
            return "🔴 Risque élevé: Surveillance étroite recommandée.";
        } else if ("MEDIUM".equals(riskLevel)) {
            return "🟡 Risque modéré: Vérification recommandée avant administration.";
        } else {
            return "🟢 Risque faible: Prescription acceptable avec suivi standard.";
        }
    }

    /**
     * Évalue la tendance du risque
     */
    private String assessRiskTrend(double averageScore) {
        if (averageScore > 60) {
            return "INCREASING";
        } else if (averageScore > 40) {
            return "STABLE";
        } else {
            return "DECREASING";
        }
    }

    /**
     * Génère une évaluation globale de la santé
     */
    private String generateHealthAssessment(long highRiskCount, long mediumRiskCount, long lowRiskCount, int total) {
        if (total == 0) {
            return "📊 Aucune prescription à analyser.";
        }

        double highPercent = (highRiskCount * 100.0) / total;
        double mediumPercent = (mediumRiskCount * 100.0) / total;

        if (highPercent > 30) {
            return "⚠️ État de santé: CRITIQUE - Plus de 30% des prescriptions à haut risque. Intervention médicale urgente recommandée.";
        } else if (highPercent > 15) {
            return "🔴 État de santé: PRÉOCCUPANT - Plusieurs prescriptions à haut risque détectées. Surveillance intensive requise.";
        } else if (mediumPercent > 40) {
            return "🟡 État de santé: ATTENTION - Risque modéré détecté. Suivi régulier recommandé.";
        } else {
            return "🟢 État de santé: BON - La plupart des prescriptions sont à faible risque.";
        }
    }

    /**
     * Génère des recommandations système
     */
    private List<String> generateRecommendations(long highRiskCount, long mediumRiskCount, int criticalCount) {
        List<String> recommendations = new ArrayList<>();

        if (criticalCount > 0) {
            recommendations.add("🚨 Identifier immédiatement les " + criticalCount + " cas critiques et contacter le prescripteur.");
        }

        if (highRiskCount > 0) {
            recommendations.add("📋 Vérifier les interactions médicamenteuses pour les " + highRiskCount + " prescriptions à haut risque.");
        }

        if (mediumRiskCount > 0) {
            recommendations.add("🔔 Documenter et surveiller les " + mediumRiskCount + " prescriptions à risque modéré.");
        }

        if (highRiskCount + mediumRiskCount == 0) {
            recommendations.add("✅ Excellent profil de sécurité. Continuer le suivi régulier.");
        }

        recommendations.add("📞 Consulter le pharmacien pour toute question sur les interactions médicamenteuses.");

        return recommendations;
    }

    /**
     * Obtient le score de risque pour une prescription spécifique
     */
    public PrescriptionRiskScoreDto getRiskScoreForPrescription(Long prescriptionId) {
        Optional<Prescription> prescription = prescriptionRepository.findById(prescriptionId);
        if (prescription.isPresent()) {
            return calculatePrescriptionRisk(prescription.get());
        }
        throw new RuntimeException("Prescription not found with id: " + prescriptionId);
    }
}
