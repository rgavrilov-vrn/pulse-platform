package ru.agroeco.pulse.auth.application;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.auth.adapter.persistence.UserRepository;
import ru.agroeco.pulse.auth.security.AppPrincipalMapper;
import ru.agroeco.pulse.auth.domain.UserEntity;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AppPrincipalMapper principalMapper;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        UserEntity user = userRepository.findByLoginIgnoreCase(username)
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new UsernameNotFoundException("User not available"));

        // Внутри транзакции lazy-load role.permissions работает
        return principalMapper.toPrincipal(user);
    }
}