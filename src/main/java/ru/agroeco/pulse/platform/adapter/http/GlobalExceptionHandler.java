package ru.agroeco.pulse.platform.adapter.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import ru.agroeco.pulse.platform.api.support.CorrelationIds;
import ru.agroeco.pulse.platform.adapter.http.dto.ErrorResponse;
import ru.agroeco.pulse.platform.domain.exception.BusinessRuleException;
import ru.agroeco.pulse.platform.domain.exception.DuplicateEventException;
import ru.agroeco.pulse.platform.domain.exception.NotFoundException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // --------------------------------------------------------------- 409/422

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> optimisticLock(OptimisticLockingFailureException ex,
                                                        HttpServletRequest req) {
        log.warn("Optimistic lock conflict: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK_CONFLICT",
                "Resource was modified by another transaction, please retry", req, null);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> businessRule(BusinessRuleException ex,
                                                      HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_CONTENT,
                "BUSINESS_RULE_VIOLATION", ex.getMessage(), req, null);
    }

    @ExceptionHandler(DuplicateEventException.class)
    public ResponseEntity<ErrorResponse> duplicate(DuplicateEventException ex,
                                                   HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_EVENT", ex.getMessage(), req, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException ex,
                                                   HttpServletRequest req) {
        // FIX #96: getMostSpecificCause().getMessage() содержит пользовательские
        // значения (например, "(code)=(SOME USER INPUT)") — PII в логе.
        // Логируем только общий тип, детали — на DEBUG (см. ex.getMostSpecificCause()
        // в локальной отладке).
        log.warn("Data integrity violation: {}", ex.getMessage());
        log.debug("Data integrity violation details", ex);
        return build(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "Request violates database constraints", req, null);
    }

    // --------------------------------------------------------------- 404

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(NotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), req, null);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> noHandler(NoHandlerFoundException ex,
                                                   HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "ENDPOINT_NOT_FOUND", "Endpoint not found", req, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> noResource(NoResourceFoundException ex,
                                                    HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource not found", req, null);
    }

    // --------------------------------------------------------------- 401/403

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> onAuthError(AuthenticationException ex,
                                                     HttpServletRequest req) {
        log.debug("Authentication failed: {}", ex.getClass().getSimpleName());
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
                "Authentication failed", req, null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> badCredentials(BadCredentialsException ex,
                                                        HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                "Invalid username or password", req, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accessDenied(AccessDeniedException ex,
                                                      HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied", req, null);
    }

    // --------------------------------------------------------------- 400

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex,
                                                    HttpServletRequest req) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Request validation failed", req, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> constraintViolation(ConstraintViolationException ex,
                                                             HttpServletRequest req) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Request validation failed", req, details);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> typeMismatch(MethodArgumentTypeMismatchException ex,
                                                      HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "Invalid value for parameter '%s'".formatted(ex.getName()), req, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> missingParameter(MissingServletRequestParameterException ex,
                                                          HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER",
                "Required parameter '%s' is missing".formatted(ex.getParameterName()), req, null);
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ErrorResponse> missingPathVariable(MissingPathVariableException ex,
                                                             HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "MISSING_PATH_VARIABLE",
                "Missing path variable '%s'".formatted(ex.getVariableName()), req, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException ex,
                                                    HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_JSON",
                "Request body is missing or malformed", req, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> methodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                            HttpServletRequest req) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "HTTP method '%s' is not supported for this endpoint".formatted(ex.getMethod()),
                req, null);
    }

    // --------------------------------------------------------------- 4xx/5xx / generic

    /**
     * FIX #97: код ошибки соответствует HTTP-статусу. Раньше всегда был
     * REQUEST_ERROR — клиент не мог отличить «неверный Content-Type» (415)
     * от «CONFLICT» (409) по коду.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> responseStatus(ResponseStatusException ex,
                                                        HttpServletRequest req) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        return build(status, codeFor(status), ex.getReason(), req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Unexpected server error", req, null);
    }

    // --------------------------------------------------------------- helpers

    /** FIX #97: осмысленные коды для ResponseStatusException. */
    private static String codeFor(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST              -> "BAD_REQUEST";
            case UNAUTHORIZED             -> "UNAUTHENTICATED";
            case FORBIDDEN                -> "ACCESS_DENIED";
            case NOT_FOUND                -> "NOT_FOUND";
            case METHOD_NOT_ALLOWED       -> "METHOD_NOT_ALLOWED";
            case NOT_ACCEPTABLE           -> "NOT_ACCEPTABLE";
            case CONFLICT                 -> "CONFLICT";
            case UNSUPPORTED_MEDIA_TYPE   -> "UNSUPPORTED_MEDIA_TYPE";
            case UNPROCESSABLE_CONTENT    -> "UNPROCESSABLE_ENTITY";
            case TOO_MANY_REQUESTS        -> "TOO_MANY_REQUESTS";
            case CONTENT_TOO_LARGE        -> "CONTENT_TOO_LARGE";
            case INTERNAL_SERVER_ERROR    -> "INTERNAL_ERROR";
            case BAD_GATEWAY              -> "BAD_GATEWAY";
            case SERVICE_UNAVAILABLE      -> "SERVICE_UNAVAILABLE";
            default -> {
                int c = status.value();
                if (c >= 500) yield "SERVER_ERROR";
                if (c >= 400) yield "CLIENT_ERROR";
                yield "UNEXPECTED_STATUS";
            }
        };
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message,
                                                HttpServletRequest req, List<String> details) {
        // FIX #95: единая точка чтения correlationId.
        String correlationId = CorrelationIds.current();
        if (correlationId == null) {
            correlationId = req.getHeader(CorrelationIds.HEADER);
        }
        return ResponseEntity.status(status).body(new ErrorResponse(
                Instant.now(), status.value(), code, message,
                details == null ? List.of() : details,
                req.getRequestURI(), correlationId));
    }
}