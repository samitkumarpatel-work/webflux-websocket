package com.example.webfluxwebsocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@SpringBootApplication
@RequiredArgsConstructor
public class WebfluxWebsocketApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebfluxWebsocketApplication.class, args);
	}

	private final BasicWebSocketHandler basicWebSocketHandler;
	private final ChatWebSocketHandler chatWebSocketHandler;

	private final OnlineHandler statusHandler;
	@Bean
	public HandlerMapping webSocketHandlerMapping() {
		Map<String, WebSocketHandler> map = new HashMap<>();
		map.put("/websocket-emitter", basicWebSocketHandler);
		map.put("/websocket-chat", chatWebSocketHandler);
		map.put("/websocket-status", statusHandler);

		SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
		handlerMapping.setOrder(1);
		handlerMapping.setUrlMap(map);
		return handlerMapping;
	}
}

@Component
class BasicWebSocketHandler implements WebSocketHandler {
	@Override
	public Mono<Void> handle(WebSocketSession session) {

		var f = Flux.interval(Duration.ofSeconds(5))
				.map(l -> session.textMessage("Hello " + l));

		return session
				.send(f)
				.and(session
						.receive()
						.map(msg -> "RECEIVED ON SERVER :: " + msg.getPayloadAsText())
						.map(session::textMessage)
						.flatMap(s -> session.send(Mono.just(s)))
						.log()
				);
	}
}

@Component
@Slf4j
class ChatWebSocketHandler implements WebSocketHandler {
	Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
	@Override
	public Mono<Void> handle(WebSocketSession session) {
		log.info("Session established: id: {}, isOpen: {} " , session.getId(), session.isOpen());

		sink.tryEmitNext("%s has joined the chat".formatted(session.getId()));
		return session
				.send(sink.asFlux().map(session::textMessage))
				.and(session.receive()
						.map(WebSocketMessage::getPayloadAsText)
						.map(s -> new Message(session.getId(), s).toString())
						.doOnNext(sink::tryEmitNext)
						.then());
	}
}

record Message(String user, String text) {}
record Member(String id, String name) {}

@Component
@Slf4j
class OnlineHandler implements WebSocketHandler {
	//private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
	private final Sinks.Many<String> sink = Sinks.many().replay().latestOrDefault("No one is online");
	private final Map<UUID, List<Member>> rooms = Collections.synchronizedMap(new HashMap<>());

	@Override
	public Mono<Void> handle(WebSocketSession session) {

		log.info("Session established: id: {}" , session.getId());
		var roomId = session.getHandshakeInfo().getUri().getQuery();
		var members = rooms.get(UUID.fromString(roomId));
		log.info("roomId: {}, members : {}", roomId,members);

		return session
			.send(sink.asFlux().map(session::textMessage))
			.and(session
					.receive()
					.map(WebSocketMessage::getPayloadAsText)
					.doOnNext(sink::tryEmitNext)
					.then());

	}

	@Bean
	public RouterFunction routerFunction() {
		return RouterFunctions
				.route()
				.path("/room", builder -> builder
						.POST("", this::createRoom)
						.GET("", this::allRooms)
						.GET("/{id}", this::roomById)
						.DELETE("/{id}", request -> ServerResponse.noContent().build())
				)
				.path("/{roomId}/member", builder -> builder
						.GET("", request -> ServerResponse.noContent().build())
						.GET("/{id}", this::memberById)
						.POST("", this::joinRoom)
						.PUT("/{id}", request -> ServerResponse.noContent().build())
						.DELETE("/{id}", request -> ServerResponse.noContent().build())
				)
				.build();
	}

	private Mono<ServerResponse> memberById(ServerRequest request) {
		var roomId = UUID.fromString(request.pathVariable("roomId"));
		var memberId = request.pathVariable("id");
		return Mono
				.fromCallable(() -> rooms.get(roomId))
				.map(members -> members
						.stream()
						.filter(m -> Objects.equals(m.id(), memberId))
						.findFirst())
				.flatMap(member -> ServerResponse.ok().bodyValue(member))
				.onErrorResume(e -> ServerResponse.badRequest().build());
	}

	private Mono<ServerResponse> roomById(ServerRequest request) {
		var roomId = UUID.fromString(request.pathVariable("id"));
		return Mono
				.fromCallable(() -> rooms.get(roomId))
				//TODO return 404 if room not found
				.filter(Objects::nonNull)
				.flatMap(members -> ServerResponse.ok().bodyValue(members))
				.onErrorResume(e -> ServerResponse.badRequest().build());
	}

	private Mono<ServerResponse> joinRoom(ServerRequest request) {
		var roomId = UUID.fromString(request.pathVariable("roomId"));
		var members = rooms.get(roomId);
		return request
				.bodyToMono(Member.class)
				.map(member -> {
					members.add(member);
					rooms.put(roomId, members);
					return member;
				})
				.doOnNext(member -> {
					log.info("Member joined: {}", member);
					sink.tryEmitNext(membersAsJson(members));
				})
				.flatMap(member -> ServerResponse.ok().bodyValue(member));
	}

	@SneakyThrows
	private String membersAsJson(List<Member> members) {
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.writeValueAsString(members);
	}

	private Mono<ServerResponse> createRoom(ServerRequest request) {
		return request
				.bodyToMono(String.class)
				.flatMap(id -> {
					var uuid = UUID.randomUUID();
					rooms.put(uuid, new ArrayList<Member>());
					return ServerResponse.ok().bodyValue(uuid);
				});
	}

	private Mono<ServerResponse> allRooms(ServerRequest request) {
		return Mono.fromCallable(rooms::keySet)
				.flatMap(ids -> ServerResponse.ok().bodyValue(ids))
				.onErrorResume(e -> ServerResponse.badRequest().bodyValue(e.getMessage()));
	}
}
