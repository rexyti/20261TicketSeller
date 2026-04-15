package com.ticketseller.infrastructure.config;

import com.ticketseller.application.LiberarReservaUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class BeanConfiguration {

    private final LiberarReservaUseCase liberarReservaUseCase;

    @PostConstruct
    public void initExpiracionJob() {
        log.info("Iniciando job de expiración de reservas cada 60 segundos");
        
        Flux.interval(Duration.ofSeconds(60))
                .flatMap(tick -> liberarReservaUseCase.ejecutar()
                        .doOnSuccess(v -> log.debug("Job de expiración ejecutado en tick {}", tick))
                        .onErrorResume(e -> {
                            log.error("Error en job de expiración", e);
                            return Mono.empty();
                        }))
                .subscribe();
    }
}
