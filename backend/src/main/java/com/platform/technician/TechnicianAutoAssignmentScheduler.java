package com.platform.technician;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "technician.assignment", name = "automatic-enabled", havingValue = "true")
public class TechnicianAutoAssignmentScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TechnicianAutoAssignmentScheduler.class);
    private final TechnicianAdminService service;
    private UUID cursor;

    public TechnicianAutoAssignmentScheduler(TechnicianAdminService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${technician.assignment.scan-interval-ms:30000}")
    public synchronized void assignConfirmedReservations() {
        List<UUID> reservationIds = service.findUnassignedConfirmedReservationIds(cursor, 100);
        if (reservationIds.isEmpty()) {
            cursor = null;
            return;
        }
        for (UUID reservationId : reservationIds) {
            try {
                service.autoAssign(reservationId);
            } catch (DataAccessException exception) {
                LOGGER.error("Automatic technician assignment failed for reservation {}",
                        reservationId, exception);
            }
        }
        cursor = reservationIds.getLast();
    }
}
