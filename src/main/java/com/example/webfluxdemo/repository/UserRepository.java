package com.example.webfluxdemo.repository;

import com.example.webfluxdemo.model.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for {@link User} entities.
 * <p>
 * Extends {@link R2dbcRepository} to provide standard CRUD operations
 * and adds a custom count query for pagination support.
 *
 * @see User
 */
public interface UserRepository extends R2dbcRepository<User, Long> {

    /**
     * Returns the total number of users in the database.
     * <p>
     * Used by {@link com.example.webfluxdemo.handler.UserHandler#listUsers} to calculate
     * the total number of pages for pagination.
     *
     * @return a {@link Mono} emitting the total user count
     */
    @Query("SELECT COUNT(*) FROM users")
    Mono<Long> countAll();
}
