package com.neuroguard.pharmacy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PharmacyDto {
    private Long id;
    private String name;
    private String address;
    private String phoneNumber;
    private Double latitude;
    private Double longitude;
    private String description;
    private Boolean openNow;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private String email;
    private Boolean hasDelivery;
    private Boolean accepts24h;
    private String specialities;
    private String imageUrl;
    private Double distance; // Distance from patient location in km
}
