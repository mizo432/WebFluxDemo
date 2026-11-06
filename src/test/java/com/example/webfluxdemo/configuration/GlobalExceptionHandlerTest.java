package com.example.webfluxdemo.configuration;

import com.example.webfluxdemo.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GlobalExceptionHandlerTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    private String authHeader() {
        return "Basic " + java.util.Base64.getEncoder().encodeToString("user:user".getBytes());
    }

    @Test
    void createUser_withBlankName_returnsProblemDetailWithValidationErrors() {
        User user = new User();
        user.setName("");
        user.setEmail("test@example.com");

        webTestClient.post()
                .uri("/router/api/users")
                .header("Authorization", authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.type").isEqualTo("https://example.com/problems/validation-error")
                .jsonPath("$.title").isEqualTo("Validation failed")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.instance").exists()
                .jsonPath("$.errors.name").isNotEmpty();
    }

    @Test
    void createUser_withBlankEmail_returnsProblemDetailWithValidationErrors() {
        User user = new User();
        user.setName("John Doe");
        user.setEmail("");

        webTestClient.post()
                .uri("/router/api/users")
                .header("Authorization", authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.type").isEqualTo("https://example.com/problems/validation-error")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.errors.email").isNotEmpty();
    }

    @Test
    void createUser_withInvalidEmail_returnsProblemDetailWithValidationErrors() {
        User user = new User();
        user.setName("John Doe");
        user.setEmail("not-an-email");

        webTestClient.post()
                .uri("/router/api/users")
                .header("Authorization", authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.type").isEqualTo("https://example.com/problems/validation-error")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.errors.email").isNotEmpty();
    }

    @Test
    void createUser_withBothFieldsBlank_returnsProblemDetailWithMultipleErrors() {
        User user = new User();
        user.setName("");
        user.setEmail("");

        webTestClient.post()
                .uri("/router/api/users")
                .header("Authorization", authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.type").isEqualTo("https://example.com/problems/validation-error")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.errors.name").isNotEmpty()
                .jsonPath("$.errors.email").isNotEmpty();
    }

    @Test
    void getUserById_withInvalidId_returnsProblemDetail() {
        webTestClient.get()
                .uri("/router/api/users/abc")
                .header("Authorization", authHeader())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.type").isEqualTo("https://example.com/problems/error")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.instance").exists();
    }

    @Test
    void getUserById_nonExistent_returnsNotFoundProblemDetail() {
        webTestClient.get()
                .uri("/router/api/users/99999")
                .header("Authorization", authHeader())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void updateUser_withBlankName_returnsProblemDetailWithValidationErrors() {
        User user = new User();
        user.setName("");
        user.setEmail("test@example.com");

        webTestClient.put()
                .uri("/router/api/users/99999")
                .header("Authorization", authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.type").isEqualTo("https://example.com/problems/validation-error")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.errors.name").isNotEmpty();
    }

    @Test
    void createUser_withNameTooLong_returnsProblemDetailWithValidationErrors() {
        User user = new User();
        user.setName("a".repeat(101));
        user.setEmail("test@example.com");

        webTestClient.post()
                .uri("/router/api/users")
                .header("Authorization", authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.type").isEqualTo("https://example.com/problems/validation-error")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.errors.name").isNotEmpty();
    }

    @Test
    void createUser_withEmailTooLong_returnsProblemDetailWithValidationErrors() {
        User user = new User();
        user.setName("John Doe");
        user.setEmail("a".repeat(200) + "@example.com");

        webTestClient.post()
                .uri("/router/api/users")
                .header("Authorization", authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.type").isEqualTo("https://example.com/problems/validation-error")
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.errors.email").isNotEmpty();
    }

    @Test
    void responseContentType_isApplicationProblemJson() {
        User user = new User();
        user.setName("");
        user.setEmail("test@example.com");

        webTestClient.post()
                .uri("/router/api/users")
                .header("Authorization", authHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
    }
}
