package ru.agroeco.pulse.platform.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.agroeco.pulse.platform.domain.event.UserAuthenticationFailed;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class LoginAuditService {

    private final ApplicationEventPublisher publisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String username, String reason) {
        publisher.publishEvent(new UserAuthenticationFailed(
                username, reason, Instant.now()));
    }
}