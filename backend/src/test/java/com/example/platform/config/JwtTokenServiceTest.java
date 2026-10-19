package com.example.platform.config;

import com.example.platform.auth.Account;
import com.example.platform.auth.AuthResponse;
import com.example.platform.auth.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenServiceTest {

    @Test
    void issuesSignedTokenWithCustomerIdentityAndExpiry() {
        byte[] secret = new byte[32];
        java.util.Arrays.fill(secret, (byte) 7);
        JwtConfig config = new JwtConfig();
        var signingKey = config.jwtSecretKey(Base64.getEncoder().encodeToString(secret));
        JwtEncoder encoder = config.jwtEncoder(signingKey);
        JwtDecoder decoder = config.jwtDecoder(signingKey);
        JwtTokenService tokenService = new JwtTokenService(encoder);
        UUID userId = UUID.randomUUID();
        Account account = new Account(
                userId, "customer@example.com", "encoded-password", "Customer", "010-1234-5678",
                "ACTIVE", List.of("CUSTOMER"));

        AuthResponse response = tokenService.createToken(account);
        Jwt jwt = decoder.decode(response.accessToken());

        assertEquals(userId.toString(), jwt.getSubject());
        assertEquals(JwtTokenService.ISSUER, jwt.getIssuer().toString());
        assertEquals(List.of("CUSTOMER"), jwt.getClaimAsStringList("roles"));
        assertEquals(JwtTokenService.TOKEN_LIFETIME_SECONDS, response.expiresIn());
    }

    @Test
    void rejectsSigningKeysShorterThanThirtyTwoBytes() {
        JwtConfig config = new JwtConfig();

        assertThrows(IllegalArgumentException.class,
                () -> config.jwtSecretKey(Base64.getEncoder().encodeToString(new byte[31])));
    }
}
