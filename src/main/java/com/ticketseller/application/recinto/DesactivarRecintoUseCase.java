package com.ticketseller.application.recinto;

import com.ticketseller.domain.exception.RecintoConEventosException;
import com.ticketseller.domain.exception.RecintoNotFoundException;
import com.ticketseller.domain.model.Recinto;
import com.ticketseller.domain.port.out.RecintoRepositoryPort;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class DesactivarRecintoUseCase {

    private final RecintoRepositoryPort recintoRepositoryPort;

    public DesactivarRecintoUseCase(RecintoRepositoryPort recintoRepositoryPort) {
        this.recintoRepositoryPort = recintoRepositoryPort;
    }

    public Mono<Recinto> ejecutar(UUID id) {
        return recintoRepositoryPort.buscarPorId(id)
                .switchIfEmpty(Mono.error(new RecintoNotFoundException("Recinto no encontrado")))
                .flatMap(recinto -> recintoRepositoryPort.tieneEventosFuturos(id)
                        .flatMap(tieneEventos -> {
                            if (tieneEventos) {
                                return Mono.error(new RecintoConEventosException("No se puede desactivar el recinto porque tiene eventos programados"));
                            }
                            return recintoRepositoryPort.guardar(recinto.toBuilder().activo(false).build());
                        }));
    }
}

