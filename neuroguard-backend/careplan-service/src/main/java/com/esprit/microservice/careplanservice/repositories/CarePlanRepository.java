package com.esprit.microservice.careplanservice.repositories;

import com.esprit.microservice.careplanservice.entities.CarePlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CarePlanRepository extends JpaRepository<CarePlan, Long> {
    List<CarePlan> findByPatientId(Long patientId);
    List<CarePlan> findByProviderId(Long providerId);

    // Statistiques 
    @Query("SELECT COUNT(cp) FROM CarePlan cp WHERE cp.patientId = :patientId")
    Long countCarePlansByPatient(@Param("patientId") Long patientId);

    @Query("SELECT COUNT(cp) FROM CarePlan cp WHERE cp.providerId = :providerId")
    Long countCarePlansByProvider(@Param("providerId") Long providerId);

    @Query("SELECT cp.nutritionStatus as status, COUNT(cp) as count FROM CarePlan cp GROUP BY cp.nutritionStatus")
    List<Object[]> getNutritionStatistics();

    @Query("SELECT cp.sleepStatus as status, COUNT(cp) as count FROM CarePlan cp GROUP BY cp.sleepStatus")
    List<Object[]> getSleepStatistics();

    @Query("SELECT cp.activityStatus as status, COUNT(cp) as count FROM CarePlan cp GROUP BY cp.activityStatus")
    List<Object[]> getActivityStatistics();

    @Query("SELECT cp.priority as priority, COUNT(cp) as count FROM CarePlan cp GROUP BY cp.priority")
    List<Object[]> getPriorityStatistics();

    @Query("SELECT COUNT(DISTINCT cp.patientId) FROM CarePlan cp")
    Long countUniquePatients();

    @Query("SELECT COUNT(cp) FROM CarePlan cp")
    Long countTotalCarePlans();

    @Query("SELECT cp.patientId, COUNT(cp) as count FROM CarePlan cp GROUP BY cp.patientId ORDER BY count DESC")
    List<Object[]> getCarePlansPerPatient();

    @Query("SELECT cp.providerId, COUNT(cp) as count FROM CarePlan cp GROUP BY cp.providerId ORDER BY count DESC")
    List<Object[]> getCarePlansPerProvider();
}