package com.example.webfluxwebsocket;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
@RequiredArgsConstructor
public class WebfluxWebsocketApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebfluxWebsocketApplication.class, args);
	}

	private final WebSocketHandler webSocketHandler;

	@Bean
	public HandlerMapping webSocketHandlerMapping() {
		Map<String, WebSocketHandler> map = new HashMap<>();
		map.put("/websocket-emitter", webSocketHandler);

		SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
		handlerMapping.setOrder(1);
		handlerMapping.setUrlMap(map);
		return handlerMapping;
	}
}

@Component
class ReactiveWebSocketHandler implements WebSocketHandler {

	@Override
	public Mono<Void> handle(WebSocketSession session) {
		return session
				.send(Flux.interval(Duration.ofSeconds(5))
						.map(l -> session.textMessage("Hello " + l))
				)
				.and(session.receive()
						.map(msg -> "RECEIVED ON SERVER :: " + msg.getPayloadAsText())
						.log()
				);
	}
}
