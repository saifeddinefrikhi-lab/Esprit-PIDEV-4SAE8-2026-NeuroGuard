package com.esprit.microservice.careplanservice.services;

import com.esprit.microservice.careplanservice.dto.CarePlanStatisticsDto;
import com.esprit.microservice.careplanservice.dto.PrescriptionStatisticsDto;
import com.esprit.microservice.careplanservice.dto.StatisticsOverviewDto;
import com.esprit.microservice.careplanservice.repositories.CarePlanRepository;
import com.esprit.microservice.careplanservice.repositories.PrescriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private CarePlanRepository carePlanRepository;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @InjectMocks
    private StatisticsService statisticsService;

    @Test
    void getCompleteStatistics_returnsAggregatedOverview() {
        when(carePlanRepository.countTotalCarePlans()).thenReturn(4L);
        when(prescriptionRepository.countTotalPrescriptions()).thenReturn(2L);
        when(carePlanRepository.countUniquePatients()).thenReturn(3L);

        when(carePlanRepository.getNutritionStatistics()).thenReturn(List.<Object[]>of(new Object[]{"TODO", 2L}));
        when(carePlanRepository.getSleepStatistics()).thenReturn(List.<Object[]>of(new Object[]{"DONE", 1L}));
        when(carePlanRepository.getActivityStatistics()).thenReturn(List.<Object[]>of(new Object[]{"TODO", 1L}));
        when(carePlanRepository.getPriorityStatistics()).thenReturn(List.<Object[]>of(new Object[]{"HIGH", 2L}));

        when(prescriptionRepository.getPrescriptionsByDate())
                .thenReturn(List.<Object[]>of(new Object[]{LocalDate.of(2026, 4, 14), 2L}));

        StatisticsOverviewDto result = statisticsService.getCompleteStatistics();

        assertEquals(4L, result.getTotalCarePlans());
        assertEquals(2L, result.getTotalPrescriptions());
        assertEquals(3L, result.getTotalActivePatients());
        assertEquals(4, result.getCarePlanStats().size());
        assertEquals(1, result.getPrescriptionStats().size());
    }

    @Test
    void getCarePlanStatistics_computesPercentages() {
        when(carePlanRepository.countTotalCarePlans()).thenReturn(10L);
        when(carePlanRepository.getNutritionStatistics()).thenReturn(List.<Object[]>of(new Object[]{"TODO", 4L}));
        when(carePlanRepository.getSleepStatistics()).thenReturn(List.of());
        when(carePlanRepository.getActivityStatistics()).thenReturn(List.of());
        when(carePlanRepository.getPriorityStatistics()).thenReturn(List.of());

        List<CarePlanStatisticsDto> stats = statisticsService.getCarePlanStatistics();

        assertEquals(1, stats.size());
        assertEquals("Nutrition: TODO", stats.get(0).getStatus());
        assertEquals(4L, stats.get(0).getCount());
        assertEquals(40.0, stats.get(0).getPercentage());
    }

    @Test
    void getPrescriptionStatistics_handlesZeroTotal() {
        when(prescriptionRepository.countTotalPrescriptions()).thenReturn(0L);
        when(prescriptionRepository.getPrescriptionsByDate())
                .thenReturn(List.<Object[]>of(new Object[]{LocalDate.of(2026, 4, 14), 1L}));

        List<PrescriptionStatisticsDto> stats = statisticsService.getPrescriptionStatistics();

        assertEquals(1, stats.size());
        assertEquals(0.0, stats.get(0).getPercentage());
    }
}
