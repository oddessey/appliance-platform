package com.platform.auth;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class JwtTokenService {

    public static final String ISSUER = "https://appliance-platform";
    public static final long TOKEN_LIFETIME_SECONDS = 1800;

    private final JwtEncoder jwtEncoder;

    public JwtTokenService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public AuthResponse createToken(Account account) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(TOKEN_LIFETIME_SECONDS))
                .subject(account.id().toString())
                .claim("email", account.email())
                .claim("roles", account.roles())
                .build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new AuthResponse(
                accessToken,
                "Bearer",
                TOKEN_LIFETIME_SECONDS,
                new AuthResponse.UserSummary(account.id(), account.email(), account.name(), List.copyOf(account.roles()))
        );
    }
}
