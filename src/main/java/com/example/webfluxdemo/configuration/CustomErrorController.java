package com.example.webfluxdemo.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webflux.error.ErrorAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.result.view.ViewResolver;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Custom error controller that renders HTML error pages for WebFlux.
 * <p>
 * Handles the {@code /error} endpoint for {@code text/html} requests,
 * passing error attributes to the Thymeleaf error view. In development
 * environments, detailed error information is exposed; in production,
 * only safe attributes are shown.
 *
 * @see ErrorAttributes
 */
@Controller
@RequestMapping
public class CustomErrorController {

    private final ErrorAttributes errorAttributes;
    private final ViewResolver viewResolver;

    /** The active Spring profile (e.g., {@code dev}, {@code prod}), used to control error detail visibility. */
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * Constructs the error controller.
     *
     * @param errorAttributes the Spring Boot error attributes provider
     * @param viewResolver the view resolver for rendering Thymeleaf templates
     */
    public CustomErrorController(ErrorAttributes errorAttributes, ViewResolver viewResolver) {
        this.errorAttributes = errorAttributes;
        this.viewResolver = viewResolver;
    }

    /**
     * Handles HTTP error responses for HTML requests.
     * <p>
     * Extracts error attributes from the request and passes them to the
     * Thymeleaf {@code error} view. The active profile is exposed as
     * {@code env} so the template can conditionally show detailed error
     * information in development environments.
     *
     * @param request the server request containing error attributes
     * @param model the model to which error attributes are added
     * @return a {@link Mono} emitting the view name ({@code "error"})
     */
    @RequestMapping(value = "/error", produces = "text/html")
    public Mono<String> handleError(ServerRequest request, Model model) {
        Map<String, Object> attributes = errorAttributes.getErrorAttributes(request, ErrorAttributeOptions.defaults());
        
        // 開発環境でのみ詳細情報を表示
        attributes.put("env", activeProfile);
        
        // attributes をテンプレートに渡す
        attributes.forEach(model::addAttribute);
        
        return Mono.just("error");
    }
}
