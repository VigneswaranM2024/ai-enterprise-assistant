package com.enterprise.assistant.security.user;

import com.enterprise.assistant.domain.user.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Custom UserDetails implementation wrapping User domain entity.
 */
@Getter
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final UUID tenantId;
    private final String tenantSlug;
    private final String email;
    private final String password;
    private final String fullName;
    private final String securityClassification;
    /** Department code from the user's assigned department (e.g. "HR", "ENGINEERING"). Null if unassigned. */
    private final String departmentCode;
    private final boolean isActive;
    private final Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal create(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        if (user.getRoles() != null) {
            for (com.enterprise.assistant.domain.user.Role role : user.getRoles()) {
                authorities.add(new SimpleGrantedAuthority(role.getName()));
                if (role.getPermissions() != null) {
                    for (com.enterprise.assistant.domain.user.Permission p : role.getPermissions()) {
                        authorities.add(new SimpleGrantedAuthority(p.getCode()));
                    }
                }
            }
        }

        String deptCode = (user.getDepartment() != null) ? user.getDepartment().getCode() : null;

        return new UserPrincipal(
                user.getId(),
                user.getTenant().getId(),
                user.getTenant().getSlug(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getSecurityClassification().name(),
                deptCode,
                user.getIsActive(),
                authorities
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }
}
