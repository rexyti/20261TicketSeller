package com.ticketseller.infrastructure.config;

import com.ticketseller.application.checkout.LiberarReservaUseCase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckoutReservaExpirationJob {

    private final LiberarReservaUseCase liberarReservaUseCase;
    private Disposable subscription;

    @PostConstruct
    public void start() {
        subscription = Flux.interval(Duration.ofMinutes(1))
                .flatMap(ignore -> liberarReservaUseCase.ejecutar())
                .onErrorContinue((throwable, ignored) -> log.error("Error liberando reservas expiradas", throwable))
                .subscribe();
    }

    @PreDestroy
    public void stop() {
        if (subscripcionNotDisposed()) {
            subscription.dispose();
        }
    }

    private boolean subscripcionNotDisposed(){
        return subscription != null && !subscription.isDisposed();
    }
}

