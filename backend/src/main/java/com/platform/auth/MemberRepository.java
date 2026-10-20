package com.platform.auth;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MemberRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Account> accountMapper = this::mapAccount;

    public MemberRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
        jdbcTemplate.query("SELECT pg_advisory_xact_lock(8080, 1)", resultSet -> {
            while (resultSet.next()) {
                resultSet.getObject(1);
            }
        });
    }

    public boolean hasAdminRole() {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM user_role WHERE role_code = 'ADMIN'
                )
                """, Boolean.class);
        return Boolean.TRUE.equals(exists);
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
            jdbcTemplate.update("""
                    INSERT INTO app_user (id, email, password_hash, name, phone, status)
                    VALUES (?, ?, ?, ?, ?, 'ACTIVE')
                    """, id, email, passwordHash, name, phone);
        } catch (DuplicateKeyException exception) {
            throw new EmailAlreadyRegisteredException();
        }
        jdbcTemplate.update("""
                INSERT INTO user_role (user_id, role_code)
                VALUES (?, ?)
                """, id, roleCode);
        if (createTechnicianProfile) {
            jdbcTemplate.update("""
                    INSERT INTO technician_profile
                        (id, user_id, approval_status, active)
                    VALUES (?, ?, 'PENDING', false)
                    """, UUID.randomUUID(), id);
        }
        return new Account(id, email, passwordHash, name, phone, "ACTIVE", 0, List.of(roleCode));
    }

    public Optional<Account> findByEmail(String email) {
        List<Account> accounts = jdbcTemplate.query("""
                SELECT id, email, password_hash, name, phone, status, auth_version
                FROM app_user
                WHERE lower(email) = lower(?) AND deleted_at IS NULL
                """, accountMapper, email);
        return withRoles(accounts);
    }

    public Optional<Account> findById(UUID id) {
        List<Account> accounts = jdbcTemplate.query("""
                SELECT id, email, password_hash, name, phone, status, auth_version
                FROM app_user
                WHERE id = ? AND deleted_at IS NULL
                """, accountMapper, id);
        return withRoles(accounts);
    }

    public List<Account> findAll(int limit, int offset) {
        List<Account> accounts = jdbcTemplate.query("""
                SELECT id, email, password_hash, name, phone, status, auth_version
                FROM app_user
                WHERE deleted_at IS NULL
                ORDER BY created_at DESC, id
                LIMIT ? OFFSET ?
                """, accountMapper, limit, offset);
        return accounts.stream().map(this::withRoles).toList();
    }

    public long countAccounts() {
        Long count = jdbcTemplate.queryForObject("""
                SELECT count(*) FROM app_user WHERE deleted_at IS NULL
                """, Long.class);
        return count == null ? 0 : count;
    }

    public void updateProfile(UUID id, String name, String phone) {
        jdbcTemplate.update("""
                UPDATE app_user
                SET name = ?, phone = ?, updated_at = now()
                WHERE id = ? AND deleted_at IS NULL
                """, name, phone, id);
    }

    public void updatePassword(UUID id, String passwordHash) {
        jdbcTemplate.update("""
                UPDATE app_user
                SET password_hash = ?, auth_version = auth_version + 1, updated_at = now()
                WHERE id = ? AND deleted_at IS NULL
                """, passwordHash, id);
    }

    public void invalidateTokens(UUID id) {
        jdbcTemplate.update("""
                UPDATE app_user
                SET auth_version = auth_version + 1, updated_at = now()
                WHERE id = ? AND deleted_at IS NULL
                """, id);
    }

    public void replaceRoles(UUID id, List<String> roles) {
        jdbcTemplate.update("DELETE FROM user_role WHERE user_id = ?", id);
        for (String role : roles) {
            jdbcTemplate.update("""
                    INSERT INTO user_role (user_id, role_code) VALUES (?, ?)
                    """, id, role);
        }
        invalidateTokens(id);
    }

    public void updateStatus(UUID id, String status) {
        jdbcTemplate.update("""
                UPDATE app_user
                SET status = ?, auth_version = auth_version + 1, updated_at = now()
                WHERE id = ? AND deleted_at IS NULL
                """, status, id);
    }

    public boolean isActiveTokenVersion(UUID id, long authVersion) {
        Boolean valid = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM app_user
                    WHERE id = ? AND status = 'ACTIVE' AND auth_version = ? AND deleted_at IS NULL
                )
                """, Boolean.class, id, authVersion);
        return Boolean.TRUE.equals(valid);
    }

    private Optional<Account> withRoles(List<Account> accounts) {
        if (accounts.isEmpty()) {
            return Optional.empty();
        }
        Account account = accounts.getFirst();
        return Optional.of(withRoles(account));
    }

    private Account withRoles(Account account) {
        List<String> roles = jdbcTemplate.query("""
                SELECT role_code
                FROM user_role
                WHERE user_id = ?
                ORDER BY role_code
                """, (resultSet, rowNumber) -> resultSet.getString("role_code"), account.id());
        return new Account(
                account.id(), account.email(), account.passwordHash(), account.name(),
                account.phone(), account.status(), account.authVersion(), roles);
    }

    public void updateLastLogin(UUID id) {
        jdbcTemplate.update("""
                UPDATE app_user
                SET last_login_at = now(), updated_at = now()
                WHERE id = ?
                """, id);
    }

    private Account mapAccount(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Account(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("email"),
                resultSet.getString("password_hash"),
                resultSet.getString("name"),
                resultSet.getString("phone"),
                resultSet.getString("status"),
                resultSet.getLong("auth_version"),
                List.of()
        );
    }
}
