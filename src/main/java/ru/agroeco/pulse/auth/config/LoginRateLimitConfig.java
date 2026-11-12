package ru.agroeco.pulse.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import ru.agroeco.pulse.auth.adapter.http.LoginRateLimitFilter;
import ru.agroeco.pulse.auth.application.LoginRateLimiterProperties;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(LoginRateLimiterProperties.class)
public class LoginRateLimitConfig {

    /**
     * Регистрируем как обычный servlet-filter ДО Spring Security
     * (UsernamePasswordAuthenticationFilter идёт позже). Иначе bcrypt
     * на каждом запросе — а rate-limit должен отсекать раньше.
     */
    @Bean
    public FilterRegistrationBean<LoginRateLimitFilter> loginRateLimitRegistration(
            LoginRateLimiterProperties props, ObjectMapper mapper) {
        var reg = new FilterRegistrationBean<>(
                new LoginRateLimitFilter(props, mapper));
        reg.addUrlPatterns("/api/v1/auth/token");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);   // после RequestLoggingFilter
        return reg;
    }
}