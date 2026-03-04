package com.neuroguard.riskalertservice.scheduler;

import com.neuroguard.riskalertservice.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlertGenerationScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlertGenerationScheduler.class);
    private final AlertService alertService;

    @Scheduled(cron = "0 0 */6 * * *") // every 6 hours
    public void generateAlerts() {
        log.info("Starting scheduled alert generation");
        alertService.generateAlertsForAllPatients();
        log.info("Finished scheduled alert generation");
    }
}