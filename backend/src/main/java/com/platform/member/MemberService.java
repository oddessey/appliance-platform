package com.platform.member;

import com.platform.auth.Account;
import com.platform.auth.AuthenticationFailedException;
import com.platform.auth.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class MemberService {

    private static final int MAX_PAGE_SIZE = 100;

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AccountResponse getProfile(UUID accountId) {
        return toResponse(findAccount(accountId));
    }

    @Transactional
    public AccountResponse updateProfile(UUID accountId, UpdateProfileRequest request) {
        String name = request.name().strip();
        String phone = request.phone().strip();
        if (name.isEmpty() || phone.isEmpty()) {
            throw new InvalidMemberRequestException();
        }
        findAccount(accountId);
        memberRepository.updateProfile(accountId, name, phone);
        return getProfile(accountId);
    }

    @Transactional
    public void changePassword(UUID accountId, ChangePasswordRequest request) {
        Account account = findAccount(accountId);
        if (request.currentPassword().getBytes(StandardCharsets.UTF_8).length > 72
                || !passwordEncoder.matches(request.currentPassword(), account.passwordHash())) {
            throw new AuthenticationFailedException();
        }
        memberRepository.updatePassword(accountId, passwordEncoder.encode(request.newPassword()));
    }

    @Transactional(readOnly = true)
    public AccountPageResponse listAccounts(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidMemberRequestException();
        }
        long offset = (long) page * size;
        if (offset > Integer.MAX_VALUE) {
            throw new InvalidMemberRequestException();
        }
        List<AccountResponse> accounts = memberRepository.findAll(size, (int) offset).stream()
                .map(this::toResponse)
                .toList();
        return new AccountPageResponse(page, size, memberRepository.countAccounts(), accounts);
    }

    @Transactional
    public AccountResponse updateRoles(UUID actorId, UUID accountId, UpdateRolesRequest request) {
        ensureNotSelfManagement(actorId, accountId);
        findAccount(accountId);
        List<String> roles = request.roles().stream()
                .map(Enum::name)
                .distinct()
                .toList();
        if (roles.size() != request.roles().size()) {
            throw new InvalidMemberRequestException();
        }
        memberRepository.replaceRoles(accountId, roles);
        return getProfile(accountId);
    }

    @Transactional
    public AccountResponse updateStatus(UUID actorId, UUID accountId, UpdateAccountStatusRequest request) {
        ensureNotSelfManagement(actorId, accountId);
        findAccount(accountId);
        memberRepository.updateStatus(accountId, request.status().name());
        return getProfile(accountId);
    }

    private Account findAccount(UUID accountId) {
        return memberRepository.findById(accountId).orElseThrow(MemberNotFoundException::new);
    }

    private void ensureNotSelfManagement(UUID actorId, UUID accountId) {
        if (actorId.equals(accountId)) {
            throw new AccountManagementConflictException();
        }
    }

    private AccountResponse toResponse(Account account) {
        return new AccountResponse(account.id(), account.email(), account.name(), account.phone(),
                account.status(), List.copyOf(account.roles()));
    }
}
