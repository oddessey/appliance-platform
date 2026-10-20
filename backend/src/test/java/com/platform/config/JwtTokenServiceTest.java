package com.platform.config;

import com.platform.auth.Account;
import com.platform.auth.AuthResponse;
import com.platform.auth.JwtTokenService;
import com.platform.auth.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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
        MemberRepository memberRepository = mock(MemberRepository.class);
        JwtDecoder decoder = config.jwtDecoder(signingKey, memberRepository);
        JwtTokenService tokenService = new JwtTokenService(encoder);
        UUID userId = UUID.randomUUID();
        Account account = new Account(
                userId, "customer@example.com", "encoded-password", "Customer", "010-1234-5678",
                "ACTIVE", 0, List.of("CUSTOMER"));
        when(memberRepository.isActiveTokenVersion(userId, 0)).thenReturn(true);

        AuthResponse response = tokenService.createToken(account);
        Jwt jwt = decoder.decode(response.accessToken());

        assertEquals(userId.toString(), jwt.getSubject());
        assertEquals(JwtTokenService.ISSUER, jwt.getIssuer().toString());
        assertEquals(List.of("CUSTOMER"), jwt.getClaimAsStringList("roles"));
        assertEquals(JwtTokenService.TOKEN_LIFETIME_SECONDS, response.expiresIn());
    }

    @Test
    void rejectsTokenAfterAccountAuthVersionChanges() {
        byte[] secret = new byte[32];
        java.util.Arrays.fill(secret, (byte) 9);
        JwtConfig config = new JwtConfig();
        var signingKey = config.jwtSecretKey(Base64.getEncoder().encodeToString(secret));
        JwtEncoder encoder = config.jwtEncoder(signingKey);
        MemberRepository memberRepository = mock(MemberRepository.class);
        JwtDecoder decoder = config.jwtDecoder(signingKey, memberRepository);
        UUID userId = UUID.randomUUID();
        Account account = new Account(
                userId, "customer@example.com", "encoded-password", "Customer", "010-1234-5678",
                "ACTIVE", 3, List.of("CUSTOMER"));
        AuthResponse response = new JwtTokenService(encoder).createToken(account);
        when(memberRepository.isActiveTokenVersion(userId, 3)).thenReturn(false);

        assertThrows(JwtException.class, () -> decoder.decode(response.accessToken()));
    }

    @Test
    void rejectsSigningKeysShorterThanThirtyTwoBytes() {
        JwtConfig config = new JwtConfig();

        assertThrows(IllegalArgumentException.class,
                () -> config.jwtSecretKey(Base64.getEncoder().encodeToString(new byte[31])));
    }
}
