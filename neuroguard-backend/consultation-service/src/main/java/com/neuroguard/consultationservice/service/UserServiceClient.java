package com.neuroguard.consultationservice.service;

import com.neuroguard.consultationservice.dto.UserDto;
import com.neuroguard.consultationservice.config.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

@FeignClient(name = "user-service",

        configuration = FeignClientConfig.class)  // <-- ajout de la configuration
public interface UserServiceClient {

    @GetMapping("/users/{id}")
    UserDto getUserById(@PathVariable("id") Long id);

    @GetMapping("/users/caregiver/{caregiverId}/patients")
    List<UserDto> getPatientsByCaregiver(@PathVariable("caregiverId") Long caregiverId);

    /**
     * Récupère tous les utilisateurs d'un rôle donné (ex: PROVIDER).
     * Inclut leurs coordonnées lat/lon pour le calcul de proximité.
     */
    @GetMapping("/users/role/{role}")
    List<UserDto> getUsersByRole(@PathVariable("role") String role);
}