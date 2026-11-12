package ru.agroeco.pulse.auth.application;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.auth.adapter.persistence.UserRepository;
import ru.agroeco.pulse.auth.domain.UserEntity;
import ru.agroeco.pulse.platform.application.security.port.UserDetailsByIdService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService, UserDetailsByIdService {

    private static final String RESERVED_SYSTEM_LOGIN = "__system__";

    private final UserRepository userRepository;
    private final AppPrincipalMapper principalMapper;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        if (RESERVED_SYSTEM_LOGIN.equals(username)) {
            throw new UsernameNotFoundException("User not available");
        }
        UserEntity user = userRepository.findByLogin(username)
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new UsernameNotFoundException("User not available"));
        return principalMapper.toPrincipal(user);
    }

    /**
     * FIX #44: JwtAuthenticationFilter использует этот метод вместо loadUserByUsername.
     * `sub=userId` — смена login'а не ломает существующие токены.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserById(UUID userId) {
        UserEntity user = userRepository.findByIdWithAuthorities(userId)
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new UsernameNotFoundException("User not available"));
        return principalMapper.toPrincipal(user);
    }
}