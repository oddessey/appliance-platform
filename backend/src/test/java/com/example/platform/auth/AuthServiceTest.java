package com.example.platform.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenService jwtTokenService;
    @InjectMocks
    private AuthService authService;

    @Test
    void signUpCreatesCustomerWithNormalizedEmailAndHashedPassword() {
        UUID userId = UUID.randomUUID();
        Account account = new Account(
                userId, "customer@example.com", "encoded-password", "Customer", "010-1234-5678",
                "ACTIVE", List.of("CUSTOMER"));
        AuthResponse expected = new AuthResponse("signed-token", "Bearer", 1800,
                new AuthResponse.UserSummary(userId, account.email(), account.name(), account.roles()));
        when(passwordEncoder.encode("Strong-pass-123")).thenReturn("encoded-password");
        when(memberRepository.create(any(UUID.class), eq("customer@example.com"), eq("encoded-password"),
                eq("Customer"), eq("010-1234-5678"))).thenReturn(account);
        when(jwtTokenService.createToken(account)).thenReturn(expected);

        AuthResponse response = authService.signUp(new CreateAccountRequest(
                " Customer@Example.com ", "Strong-pass-123", " Customer ", " 010-1234-5678 "));

        assertEquals(expected, response);
        verify(passwordEncoder).encode("Strong-pass-123");
    }

    @Test
    void signUpPropagatesDuplicateEmailFailure() {
        when(passwordEncoder.encode("Strong-pass-123")).thenReturn("encoded-password");
        when(memberRepository.create(any(UUID.class), eq("customer@example.com"), eq("encoded-password"),
                eq("Customer"), eq("010-1234-5678")))
                .thenThrow(new EmailAlreadyRegisteredException());

        assertThrows(EmailAlreadyRegisteredException.class, () -> authService.signUp(
                new CreateAccountRequest("customer@example.com", "Strong-pass-123", "Customer", "010-1234-5678")));
        verify(jwtTokenService, never()).createToken(any());
    }

    @Test
    void loginReturnsTokenForAuthenticatedAccount() {
        UUID userId = UUID.randomUUID();
        Account account = new Account(
                userId, "customer@example.com", "encoded-password", "Customer", "010-1234-5678",
                "ACTIVE", List.of("CUSTOMER"));
        AuthResponse expected = new AuthResponse("signed-token", "Bearer", 1800,
                new AuthResponse.UserSummary(userId, account.email(), account.name(), account.roles()));
        Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(
                "customer@example.com", null, List.of());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
        when(memberRepository.findByEmail("customer@example.com")).thenReturn(java.util.Optional.of(account));
        when(jwtTokenService.createToken(account)).thenReturn(expected);

        AuthResponse response = authService.login(new LoginRequest(" Customer@Example.com ", "Strong-pass-123"));

        assertEquals(expected, response);
        verify(memberRepository).updateLastLogin(userId);
    }

    @Test
    void loginReturnsGenericFailureForInvalidCredentials() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("invalid"));

        assertThrows(AuthenticationFailedException.class,
                () -> authService.login(new LoginRequest("customer@example.com", "wrong-password")));
        verify(memberRepository, never()).findByEmail(any());
        verify(jwtTokenService, never()).createToken(any());
    }

    @Test
    void loginRejectsPasswordsExceedingBcryptByteLimitBeforeAuthentication() {
        assertThrows(AuthenticationFailedException.class,
                () -> authService.login(new LoginRequest("customer@example.com", "한".repeat(25))));
        verify(authenticationManager, never()).authenticate(any(Authentication.class));
    }
}
