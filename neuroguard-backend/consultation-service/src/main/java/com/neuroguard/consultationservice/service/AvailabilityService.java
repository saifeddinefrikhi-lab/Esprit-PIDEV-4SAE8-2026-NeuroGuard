package com.neuroguard.consultationservice.service;

import com.neuroguard.consultationservice.dto.AvailabilityRequest;
import com.neuroguard.consultationservice.dto.AvailabilityResponse;
import com.neuroguard.consultationservice.entity.ProviderAvailability;
import com.neuroguard.consultationservice.exception.UnauthorizedException;
import com.neuroguard.consultationservice.repository.ProviderAvailabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AvailabilityService {

    private final ProviderAvailabilityRepository repository;

    public AvailabilityService(ProviderAvailabilityRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AvailabilityResponse create(Long providerId, AvailabilityRequest request) {
        validateTimes(request.getStartTime(), request.getEndTime());
        ProviderAvailability av = new ProviderAvailability();
        av.setProviderId(providerId);
        av.setDayOfWeek(request.getDayOfWeek());
        av.setStartTime(request.getStartTime());
        av.setEndTime(request.getEndTime());
        ProviderAvailability saved = repository.save(av);
        return mapToResponse(saved);
    }

    @Transactional
    public AvailabilityResponse update(Long id, AvailabilityRequest request, Long providerId) {
        ProviderAvailability av = repository.findById(id)
                .orElseThrow(() -> new com.neuroguard.consultationservice.exception.ResourceNotFoundException("Disponibilité non trouvée"));
        if (!av.getProviderId().equals(providerId)) {
            throw new UnauthorizedException("Vous n'êtes pas autorisé à modifier cette disponibilité");
        }
        validateTimes(request.getStartTime(), request.getEndTime());
        av.setDayOfWeek(request.getDayOfWeek());
        av.setStartTime(request.getStartTime());
        av.setEndTime(request.getEndTime());
        return mapToResponse(repository.save(av));
    }

    @Transactional
    public void delete(Long id, Long providerId) {
        ProviderAvailability av = repository.findById(id)
                .orElseThrow(() -> new com.neuroguard.consultationservice.exception.ResourceNotFoundException("Disponibilité non trouvée"));
        if (!av.getProviderId().equals(providerId)) {
            throw new UnauthorizedException("Vous n'êtes pas autorisé à supprimer cette disponibilité");
        }
        repository.delete(av);
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getByProvider(Long providerId) {
        return repository.findByProviderId(providerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AvailabilityResponse> getByProviderId(Long providerId) {
        return getByProvider(providerId);
    }

    private void validateTimes(java.time.LocalTime start, java.time.LocalTime end) {
        if (end != null && start != null && !end.isAfter(start)) {
            throw new IllegalArgumentException("L'heure de fin doit être après l'heure de début");
        }
    }

    private AvailabilityResponse mapToResponse(ProviderAvailability av) {
        AvailabilityResponse r = new AvailabilityResponse();
        r.setId(av.getId());
        r.setProviderId(av.getProviderId());
        r.setDayOfWeek(av.getDayOfWeek());
        r.setStartTime(av.getStartTime());
        r.setEndTime(av.getEndTime());
        return r;
    }
}
