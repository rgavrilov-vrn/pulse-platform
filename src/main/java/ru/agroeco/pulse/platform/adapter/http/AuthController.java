package ru.agroeco.pulse.platform.adapter.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import ru.agroeco.pulse.platform.security.AppPrincipal;
import ru.agroeco.pulse.platform.application.dto.TokenRequest;
import ru.agroeco.pulse.platform.application.dto.TokenResponse;
import ru.agroeco.pulse.platform.domain.event.UserAuthenticated;
import ru.agroeco.pulse.platform.security.JwtService;
import ru.agroeco.pulse.auth.application.LoginAuditService;

import java.time.Instant;

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
    public ResponseEntity<TokenResponse> token(

            // ВАЖНО: это swagger-аннотация, НЕ org.springframework.web.bind.annotation.RequestBody.
            // На одном параметре их должно быть ДВЕ — Spring'у для биндинга, springdoc'у для документации.
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Учётные данные",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TokenRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "admin",
                                            summary = "Administrator (login = password)",
                                            value = "{\"username\":\"admin\",\"password\":\"admin\"}"
                                    ),
                                    @ExampleObject(
                                            name = "operator",
                                            summary = "Operator",
                                            value = "{\"username\":\"operator\",\"password\":\"operator\"}"
                                    ),
                                    @ExampleObject(
                                            name = "viewer",
                                            summary = "Read-only viewer",
                                            value = "{\"username\":\"viewer\",\"password\":\"viewer\"}"
                                    )
                            }
                    )
            )@Valid @RequestBody TokenRequest request) {
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
