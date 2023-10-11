package com.example.webfluxwebsocket.repositories;

import com.example.webfluxwebsocket.models.Room;
import com.example.webfluxwebsocket.models.User;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class RoomRepository {
    static final List<Room> ROOMS = new ArrayList<>();

    public Flux<Room> findAll() {
        return Flux.fromIterable(ROOMS);
    }
    public Mono<Room> findById(String id) {
        return Mono.justOrEmpty(ROOMS.stream()
                .filter(room -> room.id().toString().equals(id))
                .findFirst());
    }
    public Mono<Room> save(Room room) {
        ROOMS.add(room);
        return Mono.just(room);
    }

    public Mono<Room> deleteById(String id) {
        return Mono.justOrEmpty(ROOMS.stream()
                .filter(room -> room.id().toString().equals(id))
                .findFirst())
                .doOnNext(ROOMS::remove);
    }

    public Mono<Room> updateById(String id, Room room) {
        return Mono.justOrEmpty(ROOMS.stream()
                .filter(r -> r.id().toString().equals(id))
                .findFirst())
                .doOnNext(r -> {
                    ROOMS.remove(r);
                    ROOMS.add(room);
                });
    }

    public Mono<Room> addUser(String id, String userId) {
        return Mono.justOrEmpty(ROOMS.stream()
                .filter(room -> room.id().toString().equals(id))
                .findFirst())
                .doOnNext(room -> {
                    List<User> users = new ArrayList<>(room.users());
                    users.add(new User(userId, "", ""));
                    ROOMS.remove(room);
                    ROOMS.add(new Room(room.id(), room.name(), users));
                });
    }
}
