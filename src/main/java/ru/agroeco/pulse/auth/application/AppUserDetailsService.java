package ru.agroeco.pulse.auth.application;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.auth.adapter.persistence.UserRepository;
import ru.agroeco.pulse.auth.domain.UserEntity;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    /** FIX #25: __system__ имеет password_hash='!', BCryptPasswordEncoder
     *  на matches() бросает IAE (не возвращает false) → 500 на логине.
     *  Отсекаем до DaoAuthenticationProvider. */
    private static final String RESERVED_SYSTEM_LOGIN = "__system__";

    private final UserRepository userRepository;
    private final AppPrincipalMapper principalMapper;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        // FIX #25
        if (RESERVED_SYSTEM_LOGIN.equals(username)) {
            throw new UsernameNotFoundException("User not available");
        }
        UserEntity user = userRepository.findByLogin(username)
                .filter(UserEntity::isActive)
                .orElseThrow(() -> new UsernameNotFoundException("User not available"));
        return principalMapper.toPrincipal(user);
    }
}