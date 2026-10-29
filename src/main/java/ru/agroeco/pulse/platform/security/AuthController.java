package ru.agroeco.pulse.platform.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import ru.agroeco.pulse.platform.application.dto.TokenRequest;
import ru.agroeco.pulse.platform.application.dto.TokenResponse;
import ru.agroeco.pulse.platform.domain.event.UserAuthenticated;
import ru.agroeco.pulse.platform.domain.event.UserAuthenticationFailed;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;
    private final LoginAuditService loginAuditService;

    @PostMapping("/token")
    @Operation(summary = "Issue JWT access token")
    public ResponseEntity<TokenResponse> token(@Valid @RequestBody TokenRequest request) {
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(), request.password()));

            AppPrincipal principal = (AppPrincipal) authentication.getPrincipal();

            // 1) сначала выпускаем токен — если упадёт, аудита успеха не будет
            String token = jwtService.generateToken(principal);
            long ttl = jwtService.expirationSeconds();

            // 2) затем фиксируем успешную аутентификацию
            eventPublisher.publishEvent(new UserAuthenticated(
                    principal.id(),
                    principal.username(),
                    Instant.now()
            ));

            return ResponseEntity.ok(new TokenResponse(token, "Bearer", ttl));

        } catch (AuthenticationException ex) {
            // 3) аудит неудачи — в отдельной транзакции, переживёт откат
            loginAuditService.recordFailure(request.username(), ex.getClass().getSimpleName());
            throw ex;
        }
    }
}
