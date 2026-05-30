package com.backend.Config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebsocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS(); // Fallback for browsers that don't support WebSocket
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix for messages FROM client TO server (e.g., /app/room.join)
        registry.setApplicationDestinationPrefixes("/app");

        // In-memory broker for /topic/** (broadcast) and /user/** (targeted)
        // For production scale, swap this with a RabbitMQ or Redis broker:
        // registry.enableStompBrokerRelay("/topic", "/user")
        //         .setRelayHost("localhost").setRelayPort(61613);
        registry.enableSimpleBroker("/topic", "/user");

        // Prefix for user-targeted messages
        registry.setUserDestinationPrefix("/user");
    }
}
