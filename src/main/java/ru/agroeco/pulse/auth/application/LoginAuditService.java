package ru.agroeco.pulse.auth.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import ru.agroeco.pulse.platform.domain.event.UserAuthenticationFailed;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoginAuditService {

    private final ApplicationEventPublisher publisher;

    /**
     * Best-effort аудит неудачной аутентификации.
     *
     * ВАЖНО: не пробрасываем исключение наружу — иначе ошибка БД в аудите
     * заменит исходный AuthenticationException на 500, и клиент не сможет
     * отличить «неверный пароль» от «сервер упал». Атакующий флудит логинами —
     * получает DoS через журнал аудита.
     *
     * Транзакцию откроет AuditEventListener (fallbackExecution=true);
     * здесь REQUIRES_NEW не нужен.
     */
    public void recordFailure(String username, String reason) {
        try {
            publisher.publishEvent(new UserAuthenticationFailed(
                    username, reason, Instant.now()));
        } catch (RuntimeException ex) {
            log.error("Auth audit failed for user={} reason={}", username, reason, ex);
            // swallow — не ломаем 401 из-за ошибки аудита
        }
    }
}