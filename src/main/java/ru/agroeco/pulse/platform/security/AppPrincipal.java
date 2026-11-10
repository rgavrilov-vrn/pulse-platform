package ru.agroeco.pulse.platform.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;

/**
 * Principal пользователя.
 *
 * FIX #20: добавлено поле {@code enabled}, отражающее auth.users.is_active.
 * Ранее {@code isEnabled()} всегда возвращал {@code true}, из-за чего
 * деактивированный пользователь с неистёкшим JWT продолжал работать.
 * Отдельного поля {@code locked} в схеме нет — используем тот же флаг.
 */
public record AppPrincipal(
        UUID id,
        String username,
        String passwordHash,
        List<GrantedAuthority> authorities,
        boolean enabled
) implements UserDetails {

    @Override public String getUsername() { return username; }
    @Override public String getPassword() { return passwordHash; }
    @Override public List<GrantedAuthority> getAuthorities() { return authorities; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return enabled; }
    @Override public boolean isCredentialsNonExpired() { return true; }

    @Override
    public String toString() {
        return "AppPrincipal[id=" + id + ", username=" + username + ", enabled=" + enabled + "]";
    }
}