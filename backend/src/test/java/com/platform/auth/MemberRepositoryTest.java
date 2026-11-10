package com.platform.auth;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberRepositoryTest {

    @Mock
    private MemberMapper memberMapper;

    @Test
    void mapperXmlDefinesAllMemberStatements() throws Exception {
        Configuration configuration = new Configuration();
        configuration.addMapper(MemberMapper.class);
        try (InputStream input = Resources.getResourceAsStream("mapper/auth/MemberMapper.xml")) {
            new XMLMapperBuilder(input, configuration, "mapper/auth/MemberMapper.xml",
                    configuration.getSqlFragments()).parse();
        }

        assertTrue(configuration.hasStatement("com.platform.auth.MemberMapper.findByEmail"));
        assertTrue(configuration.hasStatement("com.platform.auth.MemberMapper.updateLastLogin"));
    }

    @Test
    void findByEmailAddsRolesToMappedMember() {
        UUID id = UUID.randomUUID();
        MemberRow member = new MemberRow(id, "member@example.com", "hash", "Member", "555-0100", "ACTIVE", 3);
        when(memberMapper.findByEmail("member@example.com")).thenReturn(List.of(member));
        when(memberMapper.findRoles(id)).thenReturn(List.of("CUSTOMER"));

        Optional<Account> result = new MemberRepository(memberMapper).findByEmail("member@example.com");

        assertEquals(Optional.of(new Account(id, "member@example.com", "hash", "Member", "555-0100",
                "ACTIVE", 3, List.of("CUSTOMER"))), result);
    }

    @Test
    void createTechnicianInsertsRoleAndProfile() {
        UUID id = UUID.randomUUID();

        Account account = new MemberRepository(memberMapper).create(
                id, "tech@example.com", "hash", "Tech", "555-0100", SignupAccountType.TECHNICIAN);

        assertEquals(List.of("TECHNICIAN"), account.roles());
        verify(memberMapper).insertUser(id, "tech@example.com", "hash", "Tech", "555-0100");
        verify(memberMapper).insertRole(id, "TECHNICIAN");
        verify(memberMapper).insertTechnicianProfile(any(UUID.class), eq(id));
    }

    @Test
    void duplicateUserEmailIsTranslatedToDomainException() {
        UUID id = UUID.randomUUID();
        when(memberMapper.insertUser(id, "member@example.com", "hash", "Member", "555-0100"))
                .thenThrow(new DuplicateKeyException("duplicate"));

        assertThrows(EmailAlreadyRegisteredException.class, () -> new MemberRepository(memberMapper).create(
                id, "member@example.com", "hash", "Member", "555-0100", SignupAccountType.CUSTOMER));
        verify(memberMapper, never()).insertRole(any(UUID.class), any());
    }
}
