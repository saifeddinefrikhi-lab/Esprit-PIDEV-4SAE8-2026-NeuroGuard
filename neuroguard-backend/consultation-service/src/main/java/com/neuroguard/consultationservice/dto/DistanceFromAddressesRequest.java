package com.neuroguard.consultationservice.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Requête pour calculer la distance entre deux adresses (géocodage puis Haversine).
 */
public class DistanceFromAddressesRequest {
    @NotBlank(message = "L'adresse du premier point est requise")
    private String address1;

    @NotBlank(message = "L'adresse du second point est requise")
    private String address2;

    public String getAddress1() { return address1; }
    public void setAddress1(String address1) { this.address1 = address1; }

    public String getAddress2() { return address2; }
    public void setAddress2(String address2) { this.address2 = address2; }
}
