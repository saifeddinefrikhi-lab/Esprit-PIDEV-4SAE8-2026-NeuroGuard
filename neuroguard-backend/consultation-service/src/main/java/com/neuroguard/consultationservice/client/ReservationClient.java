package com.neuroguard.consultationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "reservation-service")
public interface ReservationClient {

    @GetMapping("/api/reservations/consultation/{consultationId}")
    Object getReservationByConsultationId(@PathVariable("consultationId") String consultationId);
}
