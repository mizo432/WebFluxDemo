package com.example.webfluxdemo.handler;

import com.example.webfluxdemo.model.User;
import com.example.webfluxdemo.repository.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;

@Component
public class UserHandler {

    private final UserRepository userRepository;

    public UserHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ---------- API (JSON) ----------

    public Mono<ServerResponse> getAllUsers(ServerRequest request) {
        Flux<User> users = userRepository.findAll();
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(users, User.class);
    }

    public Mono<ServerResponse> getUserById(ServerRequest request) {
        Long id = pathToLong(request, "id");
        return userRepository.findById(id)
                .flatMap(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(user))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> createUser(ServerRequest request) {
        Mono<User> user = request.bodyToMono(User.class);
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(user.flatMap(userRepository::save), User.class);
    }

    public Mono<ServerResponse> updateUser(ServerRequest request) {
        Long id = pathToLong(request, "id");
        Mono<User> user = request.bodyToMono(User.class)
                .map(u -> {
                    u.setId(id);
                    return u;
                });
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(user.flatMap(userRepository::save), User.class);
    }

    public Mono<ServerResponse> deleteUser(ServerRequest request) {
        Long id = pathToLong(request, "id");
        return userRepository.findById(id)
                .flatMap(userRepository::delete)
                .then(ServerResponse.noContent().build())
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    // ---------- Web (HTML / Thymeleaf) ----------

    public Mono<ServerResponse> listUsers(ServerRequest request) {
        Flux<User> users = userRepository.findAll();
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_HTML)
                .render("router-users", Map.of("users", users));
    }

    public Mono<ServerResponse> showCreateForm(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_HTML)
                .render("router-user-form", Map.of("user", new User()));
    }

    public Mono<ServerResponse> showEditForm(ServerRequest request) {
        Long id = pathToLong(request, "id");
        Mono<User> user = userRepository.findById(id);
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_HTML)
                .render("router-user-form", Map.of("user", user));
    }

    public Mono<ServerResponse> submitForm(ServerRequest request) {
        return request.formData().flatMap(formData -> {
            String name = formData.getFirst("name");
            String email = formData.getFirst("email");
            String idStr = formData.getFirst("id");
            User u = new User();
            u.setName(name != null ? name : "");
            u.setEmail(email != null ? email : "");
            if (idStr != null && !idStr.isEmpty()) {
                u.setId(Long.parseLong(idStr));
            }
            return userRepository.save(u)
                    .then(ServerResponse.status(302)
                            .header("Location", "/router/users")
                            .build());
        });
    }

    public Mono<ServerResponse> deleteFromForm(ServerRequest request) {
        return request.formData().flatMap(formData -> {
            String idStr = formData.getFirst("id");
            if (idStr == null || idStr.isEmpty()) {
                return ServerResponse.badRequest().bodyValue("ID is required");
            }
            Long id = Long.parseLong(idStr);
            return userRepository.findById(id)
                    .flatMap(userRepository::delete)
                    .then(ServerResponse.status(302)
                            .header("Location", "/router/users")
                            .build())
                    .switchIfEmpty(ServerResponse.notFound().build());
        });
    }

    // ---------- Helper ----------

    private static Long pathToLong(ServerRequest request, String name) {
        try {
            return Long.parseLong(request.pathVariable(name));
        } catch (NumberFormatException e) {
            throw new ServerWebInputException("Invalid path variable: " + name);
        }
    }
}
