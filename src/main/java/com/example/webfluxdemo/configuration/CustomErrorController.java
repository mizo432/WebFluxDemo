package com.example.webfluxdemo.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webflux.error.ErrorAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.result.view.ViewResolver;
import reactor.core.publisher.Mono;

import java.util.Map;

@Controller
@RequestMapping
public class CustomErrorController {

    private final ErrorAttributes errorAttributes;
    private final ViewResolver viewResolver;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    public CustomErrorController(ErrorAttributes errorAttributes, ViewResolver viewResolver) {
        this.errorAttributes = errorAttributes;
        this.viewResolver = viewResolver;
    }

    @RequestMapping(value = "/error", produces = "text/html")
    public Mono<String> handleError(ServerRequest request) {
        Map<String, Object> attributes = errorAttributes.getErrorAttributes(request, ErrorAttributeOptions.defaults());
        
        // 開発環境でのみ詳細情報を表示
        attributes.put("env", activeProfile);
        
        return Mono.just("error");
    }
}
