package com.neuroguard.consultationservice.config;

import com.neuroguard.consultationservice.service.NoOpRoadDistanceService;
import com.neuroguard.consultationservice.service.RoadDistanceService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoadDistanceServiceConfig {

    @Bean
    @ConditionalOnMissingBean(RoadDistanceService.class)
    public RoadDistanceService noOpRoadDistanceService() {
        return new NoOpRoadDistanceService();
    }
}