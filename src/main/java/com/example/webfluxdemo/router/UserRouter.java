package com.example.webfluxdemo.router;

import com.example.webfluxdemo.handler.UserHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class UserRouter {

    // ---------- API Routes (JSON) ----------

    @Bean
    public RouterFunction<ServerResponse> userApiRoutes(UserHandler userHandler) {
        return route()
                .path("/router/api/users", builder ->
                        builder.nest(accept(MediaType.APPLICATION_JSON), nested ->
                                nested.GET("", userHandler::getAllUsers)
                                        .GET("/{id}", userHandler::getUserById)
                                        .POST("", userHandler::createUser)
                                        .PUT("/{id}", userHandler::updateUser)
                                        .DELETE("/{id}", userHandler::deleteUser)
                        )
                )
                .build();
    }

    // ---------- Web Routes (HTML) ----------

    @Bean
    public RouterFunction<ServerResponse> userWebRoutes(UserHandler userHandler) {
        return route()
                .path("/router/users", builder ->
                        builder.GET("", userHandler::listUsers)
                                .GET("/table-fragment", userHandler::listUsersFragment)
                                .GET("/new", userHandler::showCreateForm)
                                .GET("/edit/{id}", userHandler::showEditForm)
                                .POST("/submit",
                                        contentType(MediaType.APPLICATION_FORM_URLENCODED),
                                        userHandler::submitForm)
                                .POST("/delete",
                                        contentType(MediaType.APPLICATION_FORM_URLENCODED),
                                        userHandler::deleteFromForm)
                )
                .build();
    }
}
