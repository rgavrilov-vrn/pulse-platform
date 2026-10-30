package ru.agroeco.pulse.platform.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.UUID;

/**
 * Principal пользователя.
 *
 * <p>Живёт в {@code platform.security}, чтобы {@code auth}, {@code audit},
 * {@code incident} и другие модули могли использовать его, не завися друг
 * от друга. Раньше был в {@code auth.security} и создавал цикл
 * {@code platform → auth} и {@code audit → auth}.</p>
 */
public record AppPrincipal(
        UUID id,
        String username,
        String passwordHash,
        List<GrantedAuthority> authorities
) implements UserDetails {

    @Override public String getUsername() { return username; }
    @Override public String getPassword() { return passwordHash; }
    @Override public List<GrantedAuthority> getAuthorities() { return authorities; }
    @Override public boolean isEnabled() { return true; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }

    @Override
    public String toString() {
        return "AppPrincipal[id=" + id + ", username=" + username + "]";
    }
}