package com.example.webfluxdemo.handler;

import com.example.webfluxdemo.model.User;
import com.example.webfluxdemo.repository.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Request handler for User CRUD operations, supporting both JSON API and HTML Web interfaces.
 * <p>
 * Provides reactive handlers for {@link User} resources routed by {@link com.example.webfluxdemo.router.UserRouter}.
 * All API methods perform manual validation and throw {@link ResponseStatusException} with
 * {@link ValidationException} as the cause for field-level error reporting via RFC 9457.
 *
 * @see com.example.webfluxdemo.router.UserRouter
 * @see com.example.webfluxdemo.configuration.GlobalExceptionHandler
 */
@Component
public class UserHandler {

    private final UserRepository userRepository;

    /** Default number of items per page for paginated queries. */
    private static final int DEFAULT_PAGE_SIZE = 10;

    /** Maximum allowed page size to prevent excessive queries. */
    private static final int MAX_PAGE_SIZE = 100;

    /** Regex pattern for validating email addresses. */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * Constructs the handler with the given repository.
     *
     * @param userRepository the reactive repository for persistent user data
     */
    public UserHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ---------- API (JSON) ----------

    /**
     * Returns all users as a JSON array.
     *
     * @param request the server request (unused)
     * @return a {@link ServerResponse} with status 200 and a JSON array of all users
     */
    public Mono<ServerResponse> getAllUsers(ServerRequest request) {
        Flux<User> users = userRepository.findAll();
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(users, User.class);
    }

    /**
     * Returns a single user by ID.
     *
     * @param request the server request containing the {@code id} path variable
     * @return a {@link ServerResponse} with status 200 and the user JSON,
     * or 404 if the user is not found
     */
    public Mono<ServerResponse> getUserById(ServerRequest request) {
        Long id = pathToLong(request, "id");
        return userRepository.findById(id)
                .flatMap(user -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(user))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    /**
     * Creates a new user from the request body.
     * <p>
     * Validates the user data before saving. If validation fails, throws
     * {@link ResponseStatusException} with {@link ValidationException} carrying
     * field-level errors.
     *
     * @param request the server request containing the user JSON body
     * @return a {@link ServerResponse} with status 200 and the created user JSON,
     * or 400 with RFC 9457 problem details if validation fails
     */
    public Mono<ServerResponse> createUser(ServerRequest request) {
        Mono<User> user = request.bodyToMono(User.class).flatMap(u -> {
            Map<String, String> errors = validateUser(u, null);
            if (!errors.isEmpty()) {
                return Mono.error(new ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Validation failed", new ValidationException(errors)));
            }
            return Mono.just(u);
        });
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(user.flatMap(userRepository::save), User.class);
    }

    /**
     * Updates an existing user by ID.
     * <p>
     * Validates the user data before saving. If validation fails, throws
     * {@link ResponseStatusException} with {@link ValidationException} carrying
     * field-level errors.
     *
     * @param request the server request containing the {@code id} path variable and user JSON body
     * @return a {@link ServerResponse} with status 200 and the updated user JSON,
     * or 400 with RFC 9457 problem details if validation fails
     */
    public Mono<ServerResponse> updateUser(ServerRequest request) {
        Long id = pathToLong(request, "id");
        Mono<User> user = request.bodyToMono(User.class).flatMap(u -> {
            u.setId(id);
            Map<String, String> errors = validateUser(u, id);
            if (!errors.isEmpty()) {
                return Mono.error(new ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Validation failed", new ValidationException(errors)));
            }
            return Mono.just(u);
        });
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(user.flatMap(userRepository::save), User.class);
    }

    /**
     * Deletes a user by ID.
     *
     * @param request the server request containing the {@code id} path variable
     * @return a {@link ServerResponse} with status 204 on success,
     * or 404 if the user is not found
     */
    public Mono<ServerResponse> deleteUser(ServerRequest request) {
        Long id = pathToLong(request, "id");
        return userRepository.findById(id)
                .flatMap(userRepository::delete)
                .then(ServerResponse.noContent().build())
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    // ---------- Web (HTML / Thymeleaf) ----------

    /**
     * Renders the paginated user list page as HTML.
     * <p>
     * Supports pagination via {@code page} and {@code size} query parameters.
     *
     * @param request the server request with optional pagination query parameters
     * @return a {@link ServerResponse} with status 200 and the rendered Thymeleaf view
     */
    public Mono<ServerResponse> listUsers(ServerRequest request) {
        int page = parsePageParam(request.queryParam("page"));
        int size = parseSizeParam(request.queryParam("size"));
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
    /**
     * Renders a paginated user list fragment for htmx partial page updates.
     * <p>
     * Returns only the table body HTML fragment, suitable for AJAX-based
     * pagination without full page reloads.
     *
     * @param request the server request with optional pagination query parameters
     * @return a {@link ServerResponse} with status 200 and the rendered table fragment
     */
    public Mono<ServerResponse> listUsersFragment(ServerRequest request) {
        int page = parsePageParam(request.queryParam("page"));
        int size = parseSizeParam(request.queryParam("size"));
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

    /**
     * Renders the user creation form as HTML.
     *
     * @param request the server request (unused)
     * @return a {@link ServerResponse} with status 200 and the rendered form view
     */
    public Mono<ServerResponse> showCreateForm(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_HTML)
                .render("router-user-form", Map.of("user", new User()));
    }

    /**
     * Renders the user edit form as HTML, pre-populated with the existing user data.
     * <p>
     * If the user is not found, redirects to the user list page.
     *
     * @param request the server request containing the {@code id} path variable
     * @return a {@link ServerResponse} with status 200 and the rendered form view,
     * or 302 redirect to the user list if not found
     */
    public Mono<ServerResponse> showEditForm(ServerRequest request) {
        Long id = pathToLong(request, "id");
        return userRepository.findById(id)
                .flatMap(user -> ServerResponse.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .render("router-user-form", Map.of("user", user)))
                .switchIfEmpty(ServerResponse.status(302)
                        .header("Location", "/router/users")
                        .build());
    }

    /**
     * Handles form submission for both creating and updating users.
     * <p>
     * Validates the submitted form data (name and email) and saves the user.
     * On validation failure, re-renders the form with error messages.
     * On success, redirects to the user list page.
     *
     * @param request the server request containing form data ({@code application/x-www-form-urlencoded})
     * @return a {@link ServerResponse} with status 302 redirect on success,
     * or 400 with the form re-rendered and error messages on validation failure
     */
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
            if (!EMAIL_PATTERN.matcher(email).matches()) {
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

    /**
     * Handles form-based user deletion.
     * <p>
     * Parses the user ID from the form data, finds and deletes the user,
     * then redirects to the user list page.
     *
     * @param request the server request containing form data with the user {@code id}
     * @return a {@link ServerResponse} with status 302 redirect on success,
     * or 400 if ID is missing, or 404 if user is not found
     */
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

    /**
     * Parses a path variable as a {@link Long}.
     *
     * @param request the server request
     * @param name the name of the path variable
     * @return the parsed long value
     * @throws ServerWebInputException if the path variable cannot be parsed as a long
     */
    private static Long pathToLong(ServerRequest request, String name) {
        try {
            return Long.parseLong(request.pathVariable(name));
        } catch (NumberFormatException e) {
            throw new ServerWebInputException("Invalid path variable: " + name);
        }
    }

    /**
     * Parses the {@code page} query parameter, defaulting to 0.
     * <p>
     * Negative values are clamped to 0.
     *
     * @param param the optional page parameter string
     * @return a non-negative page index, defaulting to 0
     */
    private static int parsePageParam(java.util.Optional<String> param) {
        try {
            int page = param.map(Integer::parseInt).orElse(0);
            return Math.max(0, page);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Parses the {@code size} query parameter, defaulting to {@link #DEFAULT_PAGE_SIZE}.
     * <p>
     * Values below 1 are clamped to 1; values above {@link #MAX_PAGE_SIZE} are clamped to the maximum.
     *
     * @param param the optional size parameter string
     * @return a page size between 1 and {@link #MAX_PAGE_SIZE}
     */
    private static int parseSizeParam(java.util.Optional<String> param) {
        try {
            int size = param.map(Integer::parseInt).orElse(DEFAULT_PAGE_SIZE);
            return Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        } catch (NumberFormatException e) {
            return DEFAULT_PAGE_SIZE;
        }
    }

    /**
     * Validates a {@link User} object and returns a map of field-level errors.
     * <p>
     * Checks that name and email are non-blank, within length limits,
     * and that the email matches the expected format.
     *
     * @param user the user to validate
     * @param id the user ID (unused, reserved for duplicate-check logic)
     * @return a map of field names to error messages; empty if validation passes
     */
    private Map<String, String> validateUser(User user, Long id) {
        Map<String, String> errors = new HashMap<>();
        if (user.getName() == null || user.getName().isBlank()) {
            errors.put("name", "名前は必須です");
        } else if (user.getName().length() > 100) {
            errors.put("name", "名前は100文字以内で指定してください");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            errors.put("email", "メールは必須です");
        } else if (user.getEmail().length() > 200) {
            errors.put("email", "メールは200文字以内で指定してください");
        } else if (!EMAIL_PATTERN.matcher(user.getEmail()).matches()) {
            errors.put("email", "正しいメールアドレスを指定してください");
        }
        return errors;
    }
}
