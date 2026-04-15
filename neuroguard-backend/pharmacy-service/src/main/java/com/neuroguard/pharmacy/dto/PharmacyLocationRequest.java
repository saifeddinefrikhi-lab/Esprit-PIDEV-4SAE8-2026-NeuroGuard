package com.neuroguard.pharmacy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PharmacyLocationRequest {
    private Double patientLatitude;
    private Double patientLongitude;
    private Integer radiusKm; // Search radius in kilometers
    private Boolean openNowOnly; // Only show open pharmacies
}
