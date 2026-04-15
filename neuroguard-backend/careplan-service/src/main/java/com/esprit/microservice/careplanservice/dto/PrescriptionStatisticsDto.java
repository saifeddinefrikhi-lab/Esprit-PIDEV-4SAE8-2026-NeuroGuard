package com.esprit.microservice.careplanservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionStatisticsDto {
    private LocalDate date;
    private Long count;
    private Double percentage;
}
