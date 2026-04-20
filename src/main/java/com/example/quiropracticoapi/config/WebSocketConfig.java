package com.example.quiropracticoapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Habilita un broker en memoria. Envía mensajes al cliente a todos los destinos con el prefijo /topic
        config.enableSimpleBroker("/topic");
        // Prefijo para los mensajes enviados DESDE el cliente hacia el servidor (@MessageMapping methods)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // El endpoint donde el cliente iniciará la conexión WebSocket
        registry.addEndpoint("/ws-kiosk")
                .setAllowedOriginPatterns("*"); // En producción configurar orígenes seguros
                // .withSockJS(); // Puedes habilitar SockJS si necesitas fallback, pero los clientes websocket modernos de flutter van directos
    }
}
