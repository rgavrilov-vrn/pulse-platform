package ru.agroeco.pulse.platform.application.security.port;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.UUID;

/**
 * FIX #44: загрузка UserDetails по внутреннему UUID, а не по username.
 * Позволяет `JwtAuthenticationFilter` работать с `sub=userId` —
 * смена login'а не ломает существующие токены.
 *
 * <p>Интерфейс объявлен в platform.security, чтобы platform не зависел от auth.</p>
 */
public interface UserDetailsByIdService {

    UserDetails loadUserById(UUID userId) throws UsernameNotFoundException;
}