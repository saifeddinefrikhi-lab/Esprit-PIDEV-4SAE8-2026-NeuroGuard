package com.neuroguard.consultationservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.neuroguard.consultationservice.entity.DayOfWeek;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public class AvailabilityRequest {
    @NotNull
    private DayOfWeek dayOfWeek;
    @NotNull
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;
    @NotNull
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
}
