package com.platform.technician;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TechnicianAssignmentRequest(@NotNull UUID technicianProfileId) {
}
