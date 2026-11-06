package com.example.webfluxdemo.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    @Value("${security.username:user}")
    private String username;

    @Value("${security.password:user}")
    private String password;

    @Value("${security.web-auth:false}")
    private boolean webAuth;

    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        return new MapReactiveUserDetailsService(
                User.builder()
                        .username(username)
                        .password(encoder.encode(password))
                        .roles("USER")
                        .build()
        );
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // CSRF: API はステートレスな HTTP Basic 認証を使用するため無効化
                // Web フォームには Thymeleaf Security Dialect で CSRF トークンを埋め込み
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> {
                    // 静的リソースは公開
                    exchange.pathMatchers("/css/**", "/error").permitAll();
                    // API エンドポイントは認証必須
                    exchange.pathMatchers("/api/**").authenticated();
                    // RouterFunction API も認証必須
                    exchange.pathMatchers("/router/api/**").authenticated();
                    // Web ページは設定に応じて認証を切り替え
                    if (webAuth) {
                        exchange.pathMatchers("/users/**", "/router/users/**").authenticated();
                    } else {
                        exchange.pathMatchers("/users/**", "/router/users/**").permitAll();
                    }
                    // BootUI 開発者用コンソールは公開
                    exchange.pathMatchers("/bootui/**").permitAll();
                    // その他は認証必須
                    exchange.anyExchange().authenticated();
                })
                .httpBasic(withDefaults())
                .build();
    }
}
