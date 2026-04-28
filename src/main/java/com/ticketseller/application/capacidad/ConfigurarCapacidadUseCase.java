package com.ticketseller.application.capacidad;

import com.ticketseller.domain.exception.CapacidadInvalidaException;
import com.ticketseller.domain.exception.recinto.RecintoConEventosException;
import com.ticketseller.domain.exception.recinto.RecintoNotFoundException;
import com.ticketseller.domain.model.recinto.Recinto;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class ConfigurarCapacidadUseCase {

    private final RecintoRepositoryPort recintoRepositoryPort;

    public Mono<Recinto> ejecutar(UUID recintoId, Integer capacidadMaxima) {
        if (capacidadMaximaNoValida(capacidadMaxima)) {
            return Mono.error(new CapacidadInvalidaException("La capacidad maxima debe ser mayor a cero"));
        }
        return recintoRepositoryPort.buscarPorId(recintoId)
                .switchIfEmpty(Mono.error(new RecintoNotFoundException("Recinto no encontrado")))
                .flatMap(recinto -> validarTicketsYGuardar(recinto, capacidadMaxima));
    }

    private boolean capacidadMaximaNoValida(Integer capacidadMaxima) {
        return capacidadMaxima == null || capacidadMaxima < 1;
    }

    private Mono<Recinto> validarTicketsYGuardar(Recinto recinto, Integer capacidadMaxima) {
        Mono<Boolean> tieneTickets = recintoTieneTicketsVendidos(recinto, capacidadMaxima);

        return tieneTickets.filter(tiene -> !tiene)
                .switchIfEmpty(Mono.error(new RecintoConEventosException("No se puede cambiar la capacidad máxima porque hay tickets vendidos")))
                .map(permitido -> buildRecintoActualizado(recinto, capacidadMaxima))
                .flatMap(recintoRepositoryPort::guardar);
    }

    private Mono<Boolean> recintoTieneTicketsVendidos(Recinto recinto, Integer capacidadMaxima) {
        boolean cambiaCapacidad = !capacidadMaxima.equals(recinto.getCapacidadMaxima());
        return cambiaCapacidad
                ? recintoRepositoryPort.tieneTicketsVendidos(recinto.getId())
                : Mono.just(false);
    }

    private Recinto buildRecintoActualizado(Recinto recinto, Integer capacidadMaxima){
        return recinto.toBuilder().capacidadMaxima(capacidadMaxima).build();
    }
}

