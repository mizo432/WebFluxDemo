package com.example.webfluxdemo.repository;

import com.example.webfluxdemo.model.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepository extends R2dbcRepository<User, Long> {

    @Query("SELECT COUNT(*) FROM users")
    Mono<Long> countAll();
}
