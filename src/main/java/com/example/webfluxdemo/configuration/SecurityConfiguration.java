package com.example.webfluxdemo.configuration;

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

    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        return new MapReactiveUserDetailsService(
                User.builder()
                        .username("user")
                        .password(encoder.encode("user"))
                        .roles("USER")
                        .build()
        );
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // CSRF: API は無効化、Web フォームは有効化
                .csrf(csrf -> csrf
                        .disable()
                )
                .authorizeExchange(exchange ->
                        exchange
                                // 静的リソースは公開
                                .pathMatchers("/css/**", "/error").permitAll()
                                // API エンドポイントは認証必須
                                .pathMatchers("/api/**").authenticated()
                                // RouterFunction API も認証必須
                                .pathMatchers("/router/api/**").authenticated()
                                // Web ページは公開（デモ用）
                                .pathMatchers("/users/**", "/router/users/**").permitAll()
                                // BootUI 開発者用コンソールは公開
                                .pathMatchers("/bootui/**").permitAll()
                                // その他は認証必須
                                .anyExchange().authenticated()
                )
                .httpBasic(withDefaults())
                .build();
    }
}
