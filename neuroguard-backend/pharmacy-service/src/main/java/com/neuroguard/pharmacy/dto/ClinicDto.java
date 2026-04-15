package com.neuroguard.pharmacy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClinicDto {
    private Long id;
    private String name;
    private String address;
    private String phoneNumber;
    private Double latitude;
    private Double longitude;
    private String description;
    private String email;
    private Boolean openNow;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private Boolean emergencyService;
    private Boolean acceptsInsurance;
    private String specialities;
    private String imageUrl;
    private Double distance;
}
