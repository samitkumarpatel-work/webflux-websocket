package com.example.webfluxwebsocket.repositories;

import com.example.webfluxwebsocket.models.User;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserRepository {
    static final List<User> USERS = new ArrayList<>();
    public Flux<User> findAll() {
        return Flux.fromIterable(USERS);
    }

    public Mono<User> findById(String id) {
        return Mono.justOrEmpty(USERS.stream()
                .filter(user -> user.id().toString().equals(id))
                .findFirst());
    }

    public Mono<User> save(User user) {
        USERS.add(user);
        return Mono.just(user);
    }

    public Mono<User> deleteById(String id) {
        return Mono.justOrEmpty(USERS.stream()
                .filter(user -> user.id().toString().equals(id))
                .findFirst())
                .doOnNext(USERS::remove);
    }

    public Mono<User> updateById(String id, User user) {
        return Mono.justOrEmpty(USERS.stream()
                .filter(u -> u.id().toString().equals(id))
                .findFirst())
                .doOnNext(u -> {
                    USERS.remove(u);
                    USERS.add(user);
                });
    }

}
