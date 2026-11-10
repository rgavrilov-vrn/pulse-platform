package ru.agroeco.pulse.platform.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ru.agroeco.pulse.auth.application.RoleAssignmentProperties;
import ru.agroeco.pulse.platform.application.dto.ErrorResponse;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
// FIX #22: регистрируем RoleAssignmentProperties (protected-roles из конфига)
@EnableConfigurationProperties({CorsProperties.class, RoleAssignmentProperties.class})
public class SecurityConfig {

    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsProperties corsProperties;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .requestCache(RequestCacheConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(
                                "/api/v1/auth/token",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/actuator/health", "/actuator/health/**",
                                "/actuator/info"
                        ).permitAll()
                        // FIX #41: Prometheus-скрейпер не умеет JWT из auth.users.
                        // Требование system:admin ломало мониторинг.
                        // Ограничение доступа — через management.server.port (отдельный
                        // порт, недоступный извне) и/или сетевой ACL.
                        // ВНИМАНИЕ: если management.server.port == server.port и порт
                        // открыт наружу — метрики публичны. Проверить в deployment.
                        .requestMatchers("/actuator/prometheus").permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/trassir/ingest"
                        ).hasAuthority("trassir:ingest")
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/trassir/admin/data"
                        ).hasAuthority("trassir:admin")
                        // FIX #26 (preventive): GET /admin/audit требует trassir:admin.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/trassir/admin/**"
                        ).hasAuthority("trassir:admin")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                writeError(res, mapper, HttpServletResponse.SC_UNAUTHORIZED,
                                        "UNAUTHENTICATED", "Authentication required"))
                        .accessDeniedHandler((req, res, ex) ->
                                writeError(res, mapper, HttpServletResponse.SC_FORBIDDEN,
                                        "ACCESS_DENIED", "Access denied")))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * FIX #43: JwtAuthenticationFilter помечен @Component, поэтому Boot
     * автоматически регистрирует его как servlet-фильтр — параллельно с
     * addFilterBefore() в SecurityFilterChain. Это даёт двойное выполнение
     * вне SecurityContextHolderFilter. Отключаем auto-registration;
     * фильтр остаётся активным только внутри security-chain.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // FIX #98 (preventive): X-Requested-With и Origin — лишние в allow-list.
        configuration.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Accept", "X-Correlation-Id"));
        configuration.setExposedHeaders(List.of("X-Correlation-Id"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration c) throws Exception {
        return c.getAuthenticationManager();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    private void writeError(HttpServletResponse res, ObjectMapper mapper,
                            int status, String code, String message) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var body = new ErrorResponse(
                Instant.now(), status, code, message,
                List.of(), null, MDC.get(CORRELATION_ID_MDC_KEY));
        mapper.writeValue(res.getOutputStream(), body);
    }
}