package com.platform.auth;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface MemberMapper {

    int acquireAdminBootstrapLock();

    boolean hasAdminRole();

    int insertUser(
            @Param("id") UUID id,
            @Param("email") String email,
            @Param("passwordHash") String passwordHash,
            @Param("name") String name,
            @Param("phone") String phone
    );

    int insertRole(@Param("userId") UUID userId, @Param("roleCode") String roleCode);

    int insertTechnicianProfile(@Param("id") UUID id, @Param("userId") UUID userId);

    List<MemberRow> findByEmail(@Param("email") String email);

    MemberRow findById(@Param("id") UUID id);

    List<MemberRow> findAll(@Param("limit") int limit, @Param("offset") int offset);

    long countAccounts();

    int updateProfile(@Param("id") UUID id, @Param("name") String name, @Param("phone") String phone);

    int updatePassword(@Param("id") UUID id, @Param("passwordHash") String passwordHash);

    int invalidateTokens(@Param("id") UUID id);

    int deleteRoles(@Param("id") UUID id);

    List<String> findRoles(@Param("userId") UUID userId);

    int updateStatus(@Param("id") UUID id, @Param("status") String status);

    boolean isActiveTokenVersion(@Param("id") UUID id, @Param("authVersion") long authVersion);

    int updateLastLogin(@Param("id") UUID id);
}
