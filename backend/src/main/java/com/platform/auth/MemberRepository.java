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

    public Account create(UUID id, String email, String passwordHash, String name, String phone) {
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
                VALUES (?, 'CUSTOMER')
                """, id);
        return new Account(id, email, passwordHash, name, phone, "ACTIVE", List.of("CUSTOMER"));
    }

    public Optional<Account> findByEmail(String email) {
        List<Account> accounts = jdbcTemplate.query("""
                SELECT id, email, password_hash, name, phone, status
                FROM app_user
                WHERE lower(email) = lower(?) AND deleted_at IS NULL
                """, accountMapper, email);
        if (accounts.isEmpty()) {
            return Optional.empty();
        }
        Account account = accounts.getFirst();
        List<String> roles = jdbcTemplate.query("""
                SELECT role_code
                FROM user_role
                WHERE user_id = ?
                ORDER BY role_code
                """, (resultSet, rowNumber) -> resultSet.getString("role_code"), account.id());
        return Optional.of(new Account(
                account.id(), account.email(), account.passwordHash(), account.name(),
                account.phone(), account.status(), roles));
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
                List.of()
        );
    }
}
