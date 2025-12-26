package com.timelessOrbit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer{
	
	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
	
		// Clients subscribe to /topic/game/{roomId}
        registry.enableSimpleBroker("/topic");
        
        // Clients send to /app/playCard, /app/join, etc.
        registry.setApplicationDestinationPrefixes("/app");

	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		
		// WebSocket handshake endpoint
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
}
