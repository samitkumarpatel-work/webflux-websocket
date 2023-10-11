package com.example.webfluxwebsocket.routers;

import com.example.webfluxwebsocket.models.Room;
import com.example.webfluxwebsocket.models.User;
import com.example.webfluxwebsocket.repositories.RoomRepository;
import com.example.webfluxwebsocket.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
public class Routers {
    final RoomRepository roomRepository;
    final UserRepository userRepository;
    @Bean
    public RouterFunction router() {
        return RouterFunctions
                .route()
                .path("/room", builder -> builder
                        .GET("", request -> ServerResponse.noContent().build())
                        .POST("", this::createRoom)
                        .GET("/{roomId}", request -> ServerResponse.noContent().build())
                        .PUT("/{roomId}", request -> ServerResponse.noContent().build())
                        .DELETE("/{roomId}", request -> ServerResponse.noContent().build())

                )
                .path("/user", builder -> builder
                        .GET("", request -> ServerResponse.noContent().build())
                        .POST("", this::createUser)
                        .GET("/{userId}", request -> ServerResponse.noContent().build())
                        .PUT("/{userId}", request -> ServerResponse.noContent().build())
                        .DELETE("/{userId}", request -> ServerResponse.noContent().build())
                )
                .build();
    }

    private Mono<ServerResponse> createUser(ServerRequest request) {
        return request
                .bodyToMono(User.class)
                .flatMap(userRepository::save)
                .flatMap(ServerResponse.ok()::bodyValue);
    }

    private Mono<ServerResponse> createRoom(ServerRequest request) {
        return request
                .bodyToMono(Room.class)
                .flatMap(roomRepository::save)
                .flatMap(ServerResponse.ok()::bodyValue);
    }
}
