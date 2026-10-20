package com.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    @Test
    void mapsJwtRolesToSpringSecurityRoleAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("00000000-0000-4000-8000-000000000001")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(1800))
                .claim("roles", List.of("CUSTOMER"))
                .build();

        var authentication = new SecurityConfig().jwtAuthenticationConverter().convert(jwt);

        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_CUSTOMER")));
    }

    @Test
    void mapsTechnicianJwtRoleToTechnicianAuthority() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .subject("00000000-0000-4000-8000-000000000001")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(1800))
                .claim("roles", List.of("TECHNICIAN"))
                .build();
        JwtAuthenticationConverter converter = new SecurityConfig().jwtAuthenticationConverter();

        var authentication = converter.convert(jwt);

        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_TECHNICIAN")));
    }
}
