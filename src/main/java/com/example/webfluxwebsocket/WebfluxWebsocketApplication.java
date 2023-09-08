package com.example.webfluxwebsocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
@RequiredArgsConstructor
public class WebfluxWebsocketApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebfluxWebsocketApplication.class, args);
	}

	private final BasicWebSocketHandler basicWebSocketHandler;
	private final ChatWebSocketHandler chatWebSocketHandler;
	@Bean
	public HandlerMapping webSocketHandlerMapping() {
		Map<String, WebSocketHandler> map = new HashMap<>();
		map.put("/websocket-emitter", basicWebSocketHandler);
		map.put("/websocket-chat", chatWebSocketHandler);

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
						.doOnNext(sink::tryEmitNext)
						.then());
	}
}