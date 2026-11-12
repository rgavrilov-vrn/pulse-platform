package ru.agroeco.pulse.platform.adapter.http;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * FIX #90: redirect на Swagger UI не должен работать в проде по умолчанию.
 * Управляется через app.swagger-ui.redirect-enabled (default: true в local,
 * false в prod). Управление — через springdoc.swagger-ui.enabled (см. application.yml).
 */
@Controller
@ConditionalOnProperty(
        name = "springdoc.swagger-ui.enabled",
        havingValue = "true",
        matchIfMissing = false)
public class HomeController {

    @GetMapping("/")
    public String redirectToSwagger() {
        return "redirect:swagger-ui/index.html";
    }
}