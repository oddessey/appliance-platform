package com.platform.auth;

import java.util.UUID;

public class MemberRow {

    private UUID id;
    private String email;
    private String passwordHash;
    private String name;
    private String phone;
    private String status;
    private long authVersion;

    public MemberRow() {
    }

    public MemberRow(
            UUID id,
            String email,
            String passwordHash,
            String name,
            String phone,
            String status,
            long authVersion
    ) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.phone = phone;
        this.status = status;
        this.authVersion = authVersion;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getAuthVersion() {
        return authVersion;
    }

    public void setAuthVersion(long authVersion) {
        this.authVersion = authVersion;
    }
}
