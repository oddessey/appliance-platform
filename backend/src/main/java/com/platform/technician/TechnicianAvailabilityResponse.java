package com.platform.technician;

import java.time.LocalTime;

public record TechnicianAvailabilityResponse(int dayOfWeek, LocalTime startTime, LocalTime endTime) {
}
