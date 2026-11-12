package ru.agroeco.pulse.auth.adapter.http;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ru.agroeco.pulse.auth.application.LoginAuditService;
import ru.agroeco.pulse.platform.adapter.http.dto.TokenRequest;
import ru.agroeco.pulse.platform.adapter.http.dto.TokenResponse;
import ru.agroeco.pulse.platform.application.event.EventDispatcher;
import ru.agroeco.pulse.platform.domain.event.UserAuthenticated;
import ru.agroeco.pulse.platform.domain.security.AppPrincipal;
import ru.agroeco.pulse.platform.application.security.JwtService;
import ru.agroeco.pulse.platform.application.security.port.TokenRevocationPort;

import java.time.Instant;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    // FIX #99: EventDispatcher вместо ApplicationEventPublisher —
    // единая точка публикации (логирование, фильтрация, tracing).
    private final EventDispatcher eventDispatcher;
    private final LoginAuditService loginAuditService;
    private final TokenRevocationPort revocation;

    @PostMapping("/token")
    @Operation(summary = "Issue JWT access token")
    @Transactional
    public ResponseEntity<TokenResponse> token(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Учётные данные",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TokenRequest.class),
                            // FIX #92: вместо реальных credentials — плейсхолдеры.
                            // Раньше в Swagger был виден работающий admin/admin.
                            examples = @ExampleObject(
                                    name = "example",
                                    summary = "Credentials (obtain in local environment)",
                                    value = "{\"username\":\"<username>\",\"password\":\"<password>\"}"
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

            // FIX #99: EventDispatcher.
            eventDispatcher.dispatch(new UserAuthenticated(
                    principal.id(), principal.username(), Instant.now()));

            return ResponseEntity.ok(new TokenResponse(token, "Bearer", ttl));

        } catch (AuthenticationException ex) {
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

    @PostMapping("/logout")
    @Operation(summary = "Revoke current access token")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = authHeader.substring(7);
            try {
                String jti = jwtService.extractJti(token);
                Instant expiresAt = jwtService.extractExpiration(token);
                revocation.revoke(jti, expiresAt, "LOGOUT");
            } catch (Exception e) {
                log.debug("Cannot parse token for logout: {}", e.getMessage());
            }
        }
        return ResponseEntity.noContent().build();
    }
}