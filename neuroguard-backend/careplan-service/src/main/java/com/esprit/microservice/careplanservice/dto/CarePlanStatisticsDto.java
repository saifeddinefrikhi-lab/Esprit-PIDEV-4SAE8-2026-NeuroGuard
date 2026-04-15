package com.esprit.microservice.careplanservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CarePlanStatisticsDto {
    private String status;
    private Long count;
    private Double percentage;
}
