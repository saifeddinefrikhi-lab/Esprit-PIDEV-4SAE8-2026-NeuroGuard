package com.esprit.microservice.careplanservice.services;

import com.esprit.microservice.careplanservice.dto.CarePlanStatisticsDto;
import com.esprit.microservice.careplanservice.dto.PrescriptionStatisticsDto;
import com.esprit.microservice.careplanservice.dto.StatisticsOverviewDto;
import com.esprit.microservice.careplanservice.repositories.CarePlanRepository;
import com.esprit.microservice.careplanservice.repositories.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    @Autowired
    private CarePlanRepository carePlanRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    /**
     * Récupère les statistiques complètes des plans de soins et prescriptions
     */
    public StatisticsOverviewDto getCompleteStatistics() {
        StatisticsOverviewDto overview = new StatisticsOverviewDto();
        
        // Statistiques générales
        overview.setTotalCarePlans(carePlanRepository.countTotalCarePlans());
        overview.setTotalPrescriptions(prescriptionRepository.countTotalPrescriptions());
        overview.setTotalActivePatients(carePlanRepository.countUniquePatients());
        
        // Statistiques détaillées
        overview.setCarePlanStats(getCarePlanStatistics());
        overview.setPrescriptionStats(getPrescriptionStatistics());
        
        return overview;
    }

    /**
     * Récupère les statistiques des plans de soins
     */
    public List<CarePlanStatisticsDto> getCarePlanStatistics() {
        List<CarePlanStatisticsDto> stats = new ArrayList<>();
        
        // Statistiques par statut nutrition
        List<Object[]> nutritionStats = carePlanRepository.getNutritionStatistics();
        long totalCarePlans = carePlanRepository.countTotalCarePlans();
        
        for (Object[] row : nutritionStats) {
            CarePlanStatisticsDto stat = new CarePlanStatisticsDto();
            stat.setStatus("Nutrition: " + row[0]);
            stat.setCount((Long) row[1]);
            stat.setPercentage(totalCarePlans > 0 ? ((Long) row[1] * 100.0 / totalCarePlans) : 0);
            stats.add(stat);
        }
        
        // Statistiques par statut sommeil
        List<Object[]> sleepStats = carePlanRepository.getSleepStatistics();
        for (Object[] row : sleepStats) {
            CarePlanStatisticsDto stat = new CarePlanStatisticsDto();
            stat.setStatus("Sleep: " + row[0]);
            stat.setCount((Long) row[1]);
            stat.setPercentage(totalCarePlans > 0 ? ((Long) row[1] * 100.0 / totalCarePlans) : 0);
            stats.add(stat);
        }
        
        // Statistiques par statut activité
        List<Object[]> activityStats = carePlanRepository.getActivityStatistics();
        for (Object[] row : activityStats) {
            CarePlanStatisticsDto stat = new CarePlanStatisticsDto();
            stat.setStatus("Activity: " + row[0]);
            stat.setCount((Long) row[1]);
            stat.setPercentage(totalCarePlans > 0 ? ((Long) row[1] * 100.0 / totalCarePlans) : 0);
            stats.add(stat);
        }
        
        // Statistiques par priorité
        List<Object[]> priorityStats = carePlanRepository.getPriorityStatistics();
        for (Object[] row : priorityStats) {
            CarePlanStatisticsDto stat = new CarePlanStatisticsDto();
            stat.setStatus("Priority: " + row[0]);
            stat.setCount((Long) row[1]);
            stat.setPercentage(totalCarePlans > 0 ? ((Long) row[1] * 100.0 / totalCarePlans) : 0);
            stats.add(stat);
        }
        
        return stats;
    }

    /**
     * Récupère les statistiques des prescriptions
     */
    public List<PrescriptionStatisticsDto> getPrescriptionStatistics() {
        List<PrescriptionStatisticsDto> stats = new ArrayList<>();
        
        List<Object[]> prescriptionByDate = prescriptionRepository.getPrescriptionsByDate();
        long totalPrescriptions = prescriptionRepository.countTotalPrescriptions();
        
        for (Object[] row : prescriptionByDate) {
            PrescriptionStatisticsDto stat = new PrescriptionStatisticsDto();
            stat.setDate((LocalDate) row[0]);
            stat.setCount((Long) row[1]);
            stat.setPercentage(totalPrescriptions > 0 ? ((Long) row[1] * 100.0 / totalPrescriptions) : 0);
            stats.add(stat);
        }
        
        return stats;
    }

    /**
     * Récupère les plans de soins par patient
     */
    public List<Object[]> getCarePlansPerPatient() {
        return carePlanRepository.getCarePlansPerPatient();
    }

    /**
     * Récupère les plans de soins par provider
     */
    public List<Object[]> getCarePlansPerProvider() {
        return carePlanRepository.getCarePlansPerProvider();
    }

    /**
     * Récupère les prescriptions par patient
     */
    public List<Object[]> getPrescriptionsPerPatient() {
        return prescriptionRepository.getPrescriptionsPerPatient();
    }

    /**
     * Récupère les prescriptions par provider
     */
    public List<Object[]> getPrescriptionsPerProvider() {
        return prescriptionRepository.getPrescriptionsPerProvider();
    }

    /**
     * Récupère le nombre de plans de soins pour un patient spécifique
     */
    public Long getCarePlanCountByPatient(Long patientId) {
        return carePlanRepository.countCarePlansByPatient(patientId);
    }

    /**
     * Récupère le nombre de prescriptions pour un patient spécifique
     */
    public Long getPrescriptionCountByPatient(Long patientId) {
        return prescriptionRepository.countPrescriptionsByPatient(patientId);
    }

    /**
     * Récupère le nombre de plans de soins pour un provider spécifique
     */
    public Long getCarePlanCountByProvider(Long providerId) {
        return carePlanRepository.countCarePlansByProvider(providerId);
    }

    /**
     * Récupère le nombre de prescriptions pour un provider spécifique
     */
    public Long getPrescriptionCountByProvider(Long providerId) {
        return prescriptionRepository.countPrescriptionsByProvider(providerId);
    }
}
