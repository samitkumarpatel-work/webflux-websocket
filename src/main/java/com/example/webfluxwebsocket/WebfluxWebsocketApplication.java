package com.example.webfluxwebsocket;

import lombok.RequiredArgsConstructor;
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
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@RequiredArgsConstructor
public class WebfluxWebsocketApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebfluxWebsocketApplication.class, args);
	}

	private final BasicWebSocketHandler basicWebSocketHandler;
	private final ChatWebSocketHandler chatWebSocketHandler;

	private final StatusHandler statusHandler;
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

@Component
@Slf4j
class StatusHandler implements WebSocketHandler {
	Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
	@Override
	public Mono<Void> handle(WebSocketSession session) {
		return session.receive()
				.doOnNext(message -> sink.tryEmitNext(message.getPayloadAsText()))
				.concatMap(message -> Mono.empty())
				.then()
				.and(session.send(sink.asFlux().map(session::textMessage)));
	}

	@Bean
	RouterFunction routerFunction() {
		return RouterFunctions
				.route()
				.path("/user", builder -> builder
						.GET("", request -> ServerResponse.noContent().build())
						.POST("", request -> {
							return request
									.bodyToMono(String.class)
									.doOnNext(sink::tryEmitNext)
									.flatMap(s -> ServerResponse.accepted().build());
						})
						.GET("/{id}", request -> ServerResponse.noContent().build())
						.PUT("/{id}", request -> ServerResponse.noContent().build())
						.DELETE("/{id}", request -> ServerResponse.noContent().build())

				)
				.build();
	}
}
