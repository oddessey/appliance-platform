package com.platform.member;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateRolesRequest(
        @NotEmpty @Size(max = 3) List<@NotNull AccountRole> roles
) {
}
