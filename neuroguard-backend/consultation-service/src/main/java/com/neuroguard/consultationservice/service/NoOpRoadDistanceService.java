package com.neuroguard.consultationservice.service;

import com.neuroguard.consultationservice.dto.DistanceMatrixResultDto;
import com.neuroguard.consultationservice.dto.GeoCoordinates;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

/**
 * Implémentation vide lorsque aucune API Distance Matrix n'est configurée.
 */

@ConditionalOnMissingBean(RoadDistanceService.class)
public class NoOpRoadDistanceService implements RoadDistanceService {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public DistanceMatrixResultDto getDistanceAndDuration(GeoCoordinates origin, GeoCoordinates destination) {
        return null;
    }
}
