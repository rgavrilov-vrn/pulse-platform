package ru.agroeco.pulse.auth.adapter.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ru.agroeco.pulse.auth.application.LoginAuditService;
import ru.agroeco.pulse.platform.application.dto.TokenRequest;
import ru.agroeco.pulse.platform.application.dto.TokenResponse;
import ru.agroeco.pulse.platform.domain.event.UserAuthenticated;
import ru.agroeco.pulse.platform.security.AppPrincipal;
import ru.agroeco.pulse.platform.security.JwtService;

import java.time.Instant;

@Slf4j
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
    // FIX #15: @Transactional нужен, чтобы @TransactionalEventListener
    // (AuditEventListener.on, NotificationEventListener.on) сработали.
    // Без транзакции AFTER_COMMIT-слушатели молчат → успешные логины
    // не аудируются.
    @Transactional
    public ResponseEntity<TokenResponse> token(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Учётные данные",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TokenRequest.class),
                            examples = @ExampleObject(
                                    name = "admin",
                                    summary = "DEV-ONLY (login = password)",
                                    value = "{\"username\":\"admin\",\"password\":\"admin\"}"
                            )
                    )
            ) @Valid @RequestBody TokenRequest request) {
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(), request.password()));

            AppPrincipal principal = (AppPrincipal) authentication.getPrincipal();

            String token = jwtService.generateToken(principal);
            long ttl = jwtService.expirationSeconds();

            eventPublisher.publishEvent(new UserAuthenticated(
                    principal.id(), principal.username(), Instant.now()));

            return ResponseEntity.ok(new TokenResponse(token, "Bearer", ttl));

        } catch (AuthenticationException ex) {
            // FIX #46: ошибка аудита не должна маскировать исходную
            // AuthenticationException (иначе 401 превращается в 500).
            try {
                loginAuditService.recordFailure(request.username(),
                        ex.getClass().getSimpleName());
            } catch (RuntimeException auditEx) {
                log.warn("Failed to record login failure audit for user={}",
                        request.username(), auditEx);
            }
            throw ex;
        }
    }
}