package com.neuroguard.assuranceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcedureSimulationDto {
    private String procedureName;
    private Double totalBaseCost;
    private Double insuranceCoverage; // Percentage (e.g., 0.8)
    private Double insuranceReimbursement; // Amount
    private Double patientRemainder; // Amount
    private String coverageLevel; // BASIC | ENHANCED | etc.
}
