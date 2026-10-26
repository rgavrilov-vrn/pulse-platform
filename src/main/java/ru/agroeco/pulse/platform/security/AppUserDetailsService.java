package ru.agroeco.pulse.platform.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.agroeco.pulse.platform.adapter.persistence.security.UserEntity;
import ru.agroeco.pulse.platform.adapter.persistence.security.UserRepository;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AppPrincipalMapper principalMapper;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        var user = userRepository.findByLoginIgnoreCase(username)
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new UsernameNotFoundException("User not available"));

        return principalMapper.toPrincipal(user);
    }
}