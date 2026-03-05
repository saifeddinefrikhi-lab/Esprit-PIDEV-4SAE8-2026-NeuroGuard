package com.neuroguard.consultationservice.repository;

import com.neuroguard.consultationservice.entity.DayOfWeek;
import com.neuroguard.consultationservice.entity.ProviderAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProviderAvailabilityRepository extends JpaRepository<ProviderAvailability, Long> {
    List<ProviderAvailability> findByProviderId(Long providerId);
    List<ProviderAvailability> findByProviderIdAndDayOfWeek(Long providerId, DayOfWeek dayOfWeek);
}
