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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class UserHandler {

    private final UserRepository userRepository;

    private static final int DEFAULT_PAGE_SIZE = 10;

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
        int page = request.queryParam("page").map(Integer::parseInt).orElse(0);
        int size = request.queryParam("size").map(Integer::parseInt).orElse(DEFAULT_PAGE_SIZE);
        int skip = page * size;

        Mono<Long> totalMono = userRepository.countAll();
        Flux<User> usersFlux = userRepository.findAll().skip(skip).take(size);

        return totalMono.flatMap(total -> {
            int totalPages = (int) Math.ceil((double) total / size);
            return usersFlux.collectList().flatMap(users -> {
                Map<String, Object> attributes = new HashMap<>();
                attributes.put("users", users);
                attributes.put("currentPage", page);
                attributes.put("pageSize", size);
                attributes.put("totalPages", totalPages);
                attributes.put("totalElements", total);
                attributes.put("hasPrevious", page > 0);
                attributes.put("hasNext", page + 1 < totalPages);
                return ServerResponse.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .render("router-users", attributes);
            });
        });
    }

    // htmx で部分的にリフレッシュするためのエンドポイント
    public Mono<ServerResponse> listUsersFragment(ServerRequest request) {
        int page = request.queryParam("page").map(Integer::parseInt).orElse(0);
        int size = request.queryParam("size").map(Integer::parseInt).orElse(DEFAULT_PAGE_SIZE);
        int skip = page * size;

        Mono<Long> totalMono = userRepository.countAll();
        Flux<User> usersFlux = userRepository.findAll().skip(skip).take(size);

        return totalMono.flatMap(total -> {
            int totalPages = (int) Math.ceil((double) total / size);
            return usersFlux.collectList().flatMap(users -> {
                Map<String, Object> attributes = new HashMap<>();
                attributes.put("users", users);
                attributes.put("currentPage", page);
                attributes.put("pageSize", size);
                attributes.put("totalPages", totalPages);
                attributes.put("totalElements", total);
                attributes.put("hasPrevious", page > 0);
                attributes.put("hasNext", page + 1 < totalPages);
                return ServerResponse.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .render("router-users-table-fragment", attributes);
            });
        });
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

            // 入力検証
            if (name == null || name.isBlank()) {
                return ServerResponse.badRequest()
                        .contentType(MediaType.TEXT_HTML)
                        .render("router-user-form", Map.of(
                                "user", new User(),
                                "nameError", "名前は必須です",
                                "emailError", email == null || email.isBlank() ? "メールは必須です" : null
                        ));
            }
            if (name.length() > 100) {
                return ServerResponse.badRequest()
                        .contentType(MediaType.TEXT_HTML)
                        .render("router-user-form", Map.of(
                                "user", new User(),
                                "nameError", "名前は100文字以内で指定してください"
                        ));
            }
            if (email == null || email.isBlank()) {
                return ServerResponse.badRequest()
                        .contentType(MediaType.TEXT_HTML)
                        .render("router-user-form", Map.of(
                                "user", new User(),
                                "emailError", "メールは必須です"
                        ));
            }
            if (email.length() > 200) {
                return ServerResponse.badRequest()
                        .contentType(MediaType.TEXT_HTML)
                        .render("router-user-form", Map.of(
                                "user", new User(),
                                "emailError", "メールは200文字以内で指定してください"
                        ));
            }
            if (!email.contains("@")) {
                return ServerResponse.badRequest()
                        .contentType(MediaType.TEXT_HTML)
                        .render("router-user-form", Map.of(
                                "user", new User(),
                                "emailError", "正しいメールアドレスを指定してください"
                        ));
            }

            User u = new User();
            u.setName(name.trim());
            u.setEmail(email.trim());
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
