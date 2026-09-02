package com.enterprise.assistant.service.search;

import com.enterprise.assistant.security.user.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SearchAuthorizationContextTest {

    @Test
    void fromUserPrincipal_NullPrincipal_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> SearchAuthorizationContext.fromUserPrincipal(null));
    }

    @Test
    void fromUserPrincipal_ValidPrincipal_ExtractsAllSecurityAttributes() {
        UUID tenantId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                UUID.randomUUID(),
                tenantId,
                "acme",
                "user@acme.com",
                "pwd",
                "User FullName",
                "RESTRICTED",
                "HR",
                true,
                List.of(
                        new SimpleGrantedAuthority("ROLE_HR_MANAGER"),
                        new SimpleGrantedAuthority("ROLE_EMPLOYEE"),
                        new SimpleGrantedAuthority("DOCUMENT_READ"),
                        new SimpleGrantedAuthority("DOCUMENT_WRITE")
                )
        );

        SearchAuthorizationContext ctx = SearchAuthorizationContext.fromUserPrincipal(principal);

        assertNotNull(ctx);
        assertEquals(tenantId, ctx.getTenantId());
        assertEquals("RESTRICTED", ctx.getUserClearance());
        assertEquals("HR", ctx.getUserDepartmentCode());
        assertEquals(2, ctx.getUserRoles().size());
        assertTrue(ctx.getUserRoles().contains("ROLE_HR_MANAGER"));
        assertTrue(ctx.getUserRoles().contains("ROLE_EMPLOYEE"));
        assertFalse(ctx.getUserRoles().contains("DOCUMENT_READ"));
    }
}
