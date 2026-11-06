package com.example.webfluxdemo.controller;

import com.example.webfluxdemo.model.User;
import com.example.webfluxdemo.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public Flux<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/{id}")
    public Mono<User> getUserById(@PathVariable Long id) {
        return userRepository.findById(id);
    }

    @PostMapping
    public Mono<User> createUser(@Valid @RequestBody Mono<User> user) {
        return user.flatMap(userRepository::save);
    }

    @PutMapping("/{id}")
    public Mono<User> updateUser(@PathVariable Long id, @Valid @RequestBody Mono<User> user) {
        return user.map(u -> {
            u.setId(id);
            return u;
        }).flatMap(userRepository::save);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .flatMap(userRepository::delete)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
