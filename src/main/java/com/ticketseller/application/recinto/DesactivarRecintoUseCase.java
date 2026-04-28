package com.ticketseller.application.recinto;

import com.ticketseller.domain.exception.recinto.RecintoConEventosException;
import com.ticketseller.domain.exception.recinto.RecintoNotFoundException;
import com.ticketseller.domain.model.recinto.Recinto;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class DesactivarRecintoUseCase {

    private final RecintoRepositoryPort recintoRepositoryPort;

    public Mono<Recinto> ejecutar(UUID id) {
        return recintoRepositoryPort.buscarPorId(id)
                .switchIfEmpty(Mono.error(new RecintoNotFoundException("Recinto no encontrado")))
                .flatMap(recinto -> recintoRepositoryPort.tieneEventosFuturos(id)
                        .filter(tiene -> !tiene)
                        .switchIfEmpty(Mono.error(new RecintoConEventosException("No se puede desactivar el recinto porque tiene eventos futuros")))
                        .map(permitido -> {
                            recinto.desactivar();
                            return recinto;
                        })
                        .flatMap(recintoRepositoryPort::guardar));
    }
}

