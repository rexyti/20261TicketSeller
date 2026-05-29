package com.ticketseller.infrastructure.config;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class SecurityContextUtils {

    private SecurityContextUtils() {}

    public static Mono<UUID> getUsuarioId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .map(UUID::fromString);
    }
}
