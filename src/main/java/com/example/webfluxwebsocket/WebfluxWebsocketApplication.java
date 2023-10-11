package com.example.webfluxwebsocket;

import com.example.webfluxwebsocket.repositories.UserRepository;
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
	public Map<String, Sinks.Many<String>> roomSinks() {
		// Map to store room IDs and their respective channels
		return new ConcurrentHashMap<>();
	}

	@Bean
	public Sinks.Many<String> globalSinks() {
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
		map.put("/ws/chat", messageHandler);
		int order = -1; // before annotated controllers
		return new SimpleUrlHandlerMapping(map, order);
	}
}

@Component
@RequiredArgsConstructor
@Slf4j
class MessageHandler implements WebSocketHandler {

	private final Sinks.Many<String> globalChannel;
	private final UserRepository userRepository;

	@Override
	public Mono<Void> handle(WebSocketSession session) {

		log.info("CLIENT {} Connected", session.getId());
		session.getAttributes().put("clientId", session.getId());

		var clientId = session.getAttributes().get("clientId");

		return session
				//send
				.send(globalChannel.asFlux().map(session::textMessage))
				//receive
				.and(session
						.receive()
						.doOnNext(d -> log.info("clientId: {} Sent an message", clientId))
						.map(WebSocketMessage::getPayloadAsText)
						.doOnNext(globalChannel::tryEmitNext)
				)
				.and(session
						.closeStatus()
						.doAfterTerminate(() -> log.info("clientId: {} Disconnected", clientId))
				)
				.then();
	}
}