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

/**
 * Router configuration for User resources, defining both JSON API and HTML Web routes.
 * <p>
 * API routes are prefixed with {@code /router/api/users} and accept JSON content.
 * Web routes are prefixed with {@code /router/users} and render Thymeleaf HTML views.
 *
 * @see com.example.webfluxdemo.handler.UserHandler
 */
@Configuration
public class UserRouter {

    // ---------- API Routes (JSON) ----------

    /**
     * Defines the JSON API routes for User CRUD operations.
     * <p>
     * Routes are nested under {@code /router/api/users} and require
     * {@code Accept: application/json} header.
     * <ul>
     *     <li>{@code GET /} — list all users</li>
     *     <li>{@code GET /{id}} — get user by ID</li>
     *     <li>{@code POST /} — create a new user</li>
     *     <li>{@code PUT /{id}} — update an existing user</li>
     *     <li>{@code DELETE /{id}} — delete a user</li>
     * </ul>
     *
     * @param userHandler the handler processing the routed requests
     * @return the configured router function for API routes
     */
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

    /**
     * Defines the HTML Web routes for User management via Thymeleaf views.
     * <p>
     * Routes are nested under {@code /router/users} and support both
     * full-page rendering and htmx fragment updates.
     * <ul>
     *     <li>{@code GET /} — paginated user list page</li>
     *     <li>{@code GET /table-fragment} — htmx table fragment for partial updates</li>
     *     <li>{@code GET /new} — user creation form</li>
     *     <li>{@code GET /edit/{id}} — user edit form</li>
     *     <li>{@code POST /submit} — handle form submission (create or update)</li>
     *     <li>{@code POST /delete} — handle form-based deletion</li>
     * </ul>
     *
     * @param userHandler the handler processing the routed requests
     * @return the configured router function for Web routes
     */
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
