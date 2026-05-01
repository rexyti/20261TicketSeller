package com.ticketseller.application.inventario;

import com.ticketseller.domain.exception.asiento.AsientoNoDisponibleException;
import com.ticketseller.domain.exception.asiento.AsientoReservadoPorOtroException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public class ReservarAsientoUseCase {

    private static final long MINUTOS_HOLD = 15;

    private final AsientoRepositoryPort asientoRepositoryPort;

    public ReservarAsientoUseCase(AsientoRepositoryPort asientoRepositoryPort) {
        this.asientoRepositoryPort = asientoRepositoryPort;
    }

    public Mono<Asiento> ejecutar(UUID asientoId) {
        return asientoRepositoryPort.buscarPorId(asientoId)
                .switchIfEmpty(Mono.error(new AsientoNoDisponibleException("Asiento no encontrado: " + asientoId)))
                .flatMap(asiento -> {
                    if (!EstadoAsiento.DISPONIBLE.equals(asiento.getEstado())) {
                        return Mono.error(new AsientoReservadoPorOtroException(
                                "ASIENTO NO DISPONIBLE - OTRO USUARIO ESTÁ COMPRANDO ESTE ASIENTO"));
                    }
                    return asientoRepositoryPort.reservarConHold(asientoId,
                            LocalDateTime.now().plusMinutes(MINUTOS_HOLD));
                })
                .onErrorMap(OptimisticLockingFailureException.class,
                        e -> new AsientoReservadoPorOtroException(
                                "ASIENTO NO DISPONIBLE - OTRO USUARIO ESTÁ COMPRANDO ESTE ASIENTO"));
    }
}
