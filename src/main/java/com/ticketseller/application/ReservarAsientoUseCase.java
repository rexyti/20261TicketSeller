package com.ticketseller.application;

import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.infrastructure.adapter.in.rest.dto.DisponibilidadResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

import com.ticketseller.domain.exception.AsientoReservadoPorOtroException;
import org.springframework.dao.OptimisticLockingFailureException;
import java.time.LocalDateTime;

public class ReservarAsientoUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;

    public ReservarAsientoUseCase(AsientoRepositoryPort asientoRepositoryPort) {
        this.asientoRepositoryPort = asientoRepositoryPort;
    }

    public Mono<DisponibilidadResponse> ejecutar(UUID asientoId, UUID ventaId) {
        LocalDateTime expiraEn = LocalDateTime.now().plusMinutes(15);
        return asientoRepositoryPort.reservarConHold(asientoId, expiraEn)
                .map(asiento -> new DisponibilidadResponse(asientoId, false, "RESERVADO"))
                .onErrorMap(OptimisticLockingFailureException.class, e -> 
                        new AsientoReservadoPorOtroException("ASIENTO NO DISPONIBLE - OTRO USUARIO ESTÁ COMPRANDO ESTE ASIENTO"));
    }
}
