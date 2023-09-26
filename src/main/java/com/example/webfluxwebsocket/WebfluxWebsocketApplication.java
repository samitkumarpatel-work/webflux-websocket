package com.example.webfluxwebsocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
public class WebfluxWebsocketApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebfluxWebsocketApplication.class, args);
	}

	@Bean
	public Map<String, WebSocketSession> userSessions() {
		// Map to store user IDs and WebSocket sessions
		return new ConcurrentHashMap<>();
	}

	@Bean
	public Map<String, Sinks.Many<String>> roomChannels() {
		// Map to store room IDs and their respective channels
		return new ConcurrentHashMap<>();
	}

	@Bean
	public Sinks.Many<String> globalChannel() {
		// Global broadcast channel
		return Sinks.many().replay().latestOrDefault("Welcome!");
	}
}

@Configuration
@RequiredArgsConstructor
class WebSocketConfig {

	private final MessageHandler messageHandler;
	@Bean
	public HandlerMapping handlerMapping() {
		Map<String, WebSocketHandler> map = new HashMap<>();
		map.put("/ws/message", messageHandler);
		int order = -1; // before annotated controllers
		return new SimpleUrlHandlerMapping(map, order);
	}
}

@Component
@RequiredArgsConstructor
@Slf4j
class MessageHandler implements WebSocketHandler {
	private final Sinks.Many<String> globalChannel;
	@Override
	public Mono<Void> handle(WebSocketSession session) {

		log.info("CLIENT {} connected", session.getId());

		session.getAttributes().put("id", session.getId());

		return session
				//send
				.send(globalChannel.asFlux().map(session::textMessage))
				//receive
				.and(session
						.receive()
						.doOnNext(d -> log.info("USER: {} sent an message", session.getAttributes().get("id")))
						.map(WebSocketMessage::getPayloadAsText)
						.doOnNext(globalChannel::tryEmitNext)
				)
				.and(session
						.closeStatus()
						.doAfterTerminate(() -> log.info("USER: {} is disconnected", session.getAttributes().get("id")))
				)
				.then();
	}
}