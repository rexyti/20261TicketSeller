package com.ticketseller.application.capacidad;

import com.ticketseller.domain.exception.CapacidadInvalidaException;
import com.ticketseller.domain.exception.RecintoConEventosException;
import com.ticketseller.domain.exception.RecintoNotFoundException;
import com.ticketseller.domain.model.Recinto;
import com.ticketseller.domain.port.out.RecintoRepositoryPort;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class ConfigurarCapacidadUseCase {

    private final RecintoRepositoryPort recintoRepositoryPort;

    public ConfigurarCapacidadUseCase(RecintoRepositoryPort recintoRepositoryPort) {
        this.recintoRepositoryPort = recintoRepositoryPort;
    }

    public Mono<Recinto> ejecutar(UUID recintoId, Integer capacidadMaxima) {
        if (capacidadMaxima == null || capacidadMaxima < 1) {
            return Mono.error(new CapacidadInvalidaException("La capacidad maxima debe ser mayor a cero"));
        }
        return recintoRepositoryPort.buscarPorId(recintoId)
                .switchIfEmpty(Mono.error(new RecintoNotFoundException("Recinto no encontrado")))
                .flatMap(recinto -> validarTicketsYGuardar(recinto, capacidadMaxima));
    }

    private Mono<Recinto> validarTicketsYGuardar(Recinto recinto, Integer capacidadMaxima) {
        boolean cambiaCapacidad = !capacidadMaxima.equals(recinto.getCapacidadMaxima());
        Mono<Boolean> tieneTickets = cambiaCapacidad
                ? recintoRepositoryPort.tieneTicketsVendidos(recinto.getId())
                : Mono.just(false);

        return tieneTickets.flatMap(vendidos -> {
            if (vendidos) {
                return Mono.error(new RecintoConEventosException("No se puede cambiar la capacidad porque existen tickets vendidos"));
            }
            Recinto actualizado = recinto.toBuilder().capacidadMaxima(capacidadMaxima).build();
            return recintoRepositoryPort.guardar(actualizado);
        });
    }
}

