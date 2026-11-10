package com.platform.auth;

import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MemberRepository {

    private final MemberMapper memberMapper;

    public MemberRepository(MemberMapper memberMapper) {
        this.memberMapper = memberMapper;
    }

    public Account create(
            UUID id,
            String email,
            String passwordHash,
            String name,
            String phone,
            SignupAccountType accountType
    ) {
        return createWithRole(id, email, passwordHash, name, phone, accountType.name(),
                accountType == SignupAccountType.TECHNICIAN);
    }

    public Account createAdmin(
            UUID id,
            String email,
            String passwordHash,
            String name,
            String phone
    ) {
        return createWithRole(id, email, passwordHash, name, phone, "ADMIN", false);
    }

    public void acquireAdminBootstrapLock() {
        memberMapper.acquireAdminBootstrapLock();
    }

    public boolean hasAdminRole() {
        return memberMapper.hasAdminRole();
    }

    private Account createWithRole(
            UUID id,
            String email,
            String passwordHash,
            String name,
            String phone,
            String roleCode,
            boolean createTechnicianProfile
    ) {
        try {
            memberMapper.insertUser(id, email, passwordHash, name, phone);
        } catch (RuntimeException exception) {
            if (isDuplicateKey(exception)) {
                throw new EmailAlreadyRegisteredException();
            }
            throw exception;
        }
        memberMapper.insertRole(id, roleCode);
        if (createTechnicianProfile) {
            memberMapper.insertTechnicianProfile(UUID.randomUUID(), id);
        }
        return new Account(id, email, passwordHash, name, phone, "ACTIVE", 0, List.of(roleCode));
    }

    public Optional<Account> findByEmail(String email) {
        return memberMapper.findByEmail(email).stream().findFirst().map(this::withRoles);
    }

    public Optional<Account> findById(UUID id) {
        return Optional.ofNullable(memberMapper.findById(id)).map(this::withRoles);
    }

    public List<Account> findAll(int limit, int offset) {
        return memberMapper.findAll(limit, offset).stream().map(this::withRoles).toList();
    }

    public long countAccounts() {
        return memberMapper.countAccounts();
    }

    public void updateProfile(UUID id, String name, String phone) {
        memberMapper.updateProfile(id, name, phone);
    }

    public void updatePassword(UUID id, String passwordHash) {
        memberMapper.updatePassword(id, passwordHash);
    }

    public void invalidateTokens(UUID id) {
        memberMapper.invalidateTokens(id);
    }

    public void replaceRoles(UUID id, List<String> roles) {
        memberMapper.deleteRoles(id);
        for (String role : roles) {
            memberMapper.insertRole(id, role);
        }
        invalidateTokens(id);
    }

    public void updateStatus(UUID id, String status) {
        memberMapper.updateStatus(id, status);
    }

    public boolean isActiveTokenVersion(UUID id, long authVersion) {
        return memberMapper.isActiveTokenVersion(id, authVersion);
    }

    public void updateLastLogin(UUID id) {
        memberMapper.updateLastLogin(id);
    }

    private Account withRoles(MemberRow member) {
        return new Account(
                member.getId(), member.getEmail(), member.getPasswordHash(), member.getName(),
                member.getPhone(), member.getStatus(), member.getAuthVersion(),
                memberMapper.findRoles(member.getId()));
    }

    private boolean isDuplicateKey(RuntimeException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            for (Class<?> exceptionType = cause.getClass();
                    exceptionType != null;
                    exceptionType = exceptionType.getSuperclass()) {
                if ("org.springframework.dao.DuplicateKeyException".equals(exceptionType.getName())) {
                    return true;
                }
            }
            if (cause instanceof SQLException sqlException && "23505".equals(sqlException.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}
