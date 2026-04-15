package com.esprit.microservice.careplanservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsOverviewDto {
    private Long totalCarePlans;
    private Long totalPrescriptions;
    private Long totalActivePatients;
    private List<CarePlanStatisticsDto> carePlanStats;
    private List<PrescriptionStatisticsDto> prescriptionStats;
}
