package com.platform.member;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminMemberController {

    private final MemberService memberService;

    public AdminMemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public AccountPageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return memberService.listAccounts(page, size);
    }

    @PatchMapping("/{id}/roles")
    public AccountResponse updateRoles(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRolesRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return memberService.updateRoles(accountId(jwt), id, request);
    }

    @PatchMapping("/{id}/status")
    public AccountResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAccountStatusRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return memberService.updateStatus(accountId(jwt), id, request);
    }

    private UUID accountId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
