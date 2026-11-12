package ru.agroeco.pulse.auth.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.platform.domain.event.UserAuthenticationFailed;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoginAuditService {

    // FIX #100: EventDispatcher вместо ApplicationEventPublisher —
    // единый путь публикации через debug-логирование, фильтрацию,
    // (потенциально) tracing.
    private final EventDispatcher events;

    public void recordFailure(String username, String reason) {
        try {
            events.dispatch(new UserAuthenticationFailed(
                    username, reason, Instant.now()));
        } catch (RuntimeException ex) {
            log.error("Auth audit failed for user={} reason={}", username, reason, ex);
            // swallow — не ломаем 401 из-за ошибки аудита
        }
    }
}