package com.neuroguard.assuranceservice.controller;

import com.neuroguard.assuranceservice.dto.ProcedureSimulationDto;
import com.neuroguard.assuranceservice.dto.SimulationResponseDto;
import com.neuroguard.assuranceservice.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulations")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;

    @GetMapping("/procedure")
    public ResponseEntity<ProcedureSimulationDto> simulateProcedure(
            @RequestParam Long patientId,
            @RequestParam String procedureName) {
        return ResponseEntity.ok(simulationService.simulateProcedure(patientId, procedureName));
    }

    @GetMapping("/rentability/{patientId}")
    public ResponseEntity<SimulationResponseDto> getRentabilityAnalysis(@PathVariable Long patientId) {
        return ResponseEntity.ok(simulationService.getRentabilityAnalysis(patientId));
    }
}
