package com.platform.member;

import com.platform.auth.Account;
import com.platform.auth.AuthenticationFailedException;
import com.platform.auth.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @InjectMocks
    private MemberService memberService;

    @Test
    void returnsProfileWithoutPasswordHash() {
        Account account = account(List.of("CUSTOMER"));
        when(memberRepository.findById(account.id())).thenReturn(Optional.of(account));

        AccountResponse response = memberService.getProfile(account.id());

        assertEquals(account.id(), response.id());
        assertEquals(account.email(), response.email());
        assertEquals(account.roles(), response.roles());
    }

    @Test
    void updatesOnlyNameAndPhoneAfterTrimmingInput() {
        Account account = account(List.of("CUSTOMER"));
        when(memberRepository.findById(account.id())).thenReturn(Optional.of(account));

        memberService.updateProfile(account.id(), new UpdateProfileRequest(" New Name ", " 010-9876-5432 "));

        verify(memberRepository).updateProfile(account.id(), "New Name", "010-9876-5432");
    }

    @Test
    void changesPasswordOnlyAfterCurrentPasswordMatches() {
        Account account = account(List.of("CUSTOMER"));
        when(memberRepository.findById(account.id())).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("current-password", account.passwordHash())).thenReturn(true);
        when(passwordEncoder.encode("new-password-123")).thenReturn("new-hash");

        memberService.changePassword(account.id(), new ChangePasswordRequest(
                "current-password", "new-password-123"));

        verify(memberRepository).updatePassword(account.id(), "new-hash");
    }

    @Test
    void rejectsPasswordChangeWhenCurrentPasswordDoesNotMatch() {
        Account account = account(List.of("CUSTOMER"));
        when(memberRepository.findById(account.id())).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong-password", account.passwordHash())).thenReturn(false);

        assertThrows(AuthenticationFailedException.class, () -> memberService.changePassword(
                account.id(), new ChangePasswordRequest("wrong-password", "new-password-123")));
        verify(memberRepository, never()).updatePassword(eq(account.id()), anyString());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void preventsAdministratorsFromChangingTheirOwnRoleOrStatus() {
        UUID accountId = UUID.randomUUID();

        assertThrows(AccountManagementConflictException.class, () -> memberService.updateRoles(
                accountId, accountId, new UpdateRolesRequest(List.of(AccountRole.CUSTOMER))));
        assertThrows(AccountManagementConflictException.class, () -> memberService.updateStatus(
                accountId, accountId, new UpdateAccountStatusRequest(AccountStatus.SUSPENDED)));
    }

    @Test
    void replacesRolesForAnotherAccount() {
        UUID actorId = UUID.randomUUID();
        Account account = account(List.of("CUSTOMER"));
        when(memberRepository.findById(account.id())).thenReturn(Optional.of(account));
        UpdateRolesRequest request = new UpdateRolesRequest(List.of(AccountRole.TECHNICIAN, AccountRole.ADMIN));

        memberService.updateRoles(actorId, account.id(), request);

        verify(memberRepository).replaceRoles(account.id(), List.of("TECHNICIAN", "ADMIN"));
    }

    @Test
    void changesAccountStatusForAnotherAccount() {
        UUID actorId = UUID.randomUUID();
        Account account = account(List.of("CUSTOMER"));
        when(memberRepository.findById(account.id())).thenReturn(Optional.of(account));

        memberService.updateStatus(actorId, account.id(),
                new UpdateAccountStatusRequest(AccountStatus.SUSPENDED));

        verify(memberRepository).updateStatus(account.id(), "SUSPENDED");
    }

    private Account account(List<String> roles) {
        return new Account(UUID.randomUUID(), "member@example.com", "password-hash", "Member",
                "010-1234-5678", "ACTIVE", 0, roles);
    }
}
