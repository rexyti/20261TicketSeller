package com.ticketseller.application.asiento;

import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.shared.Pagina;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarMapaAsientosUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;

    public Mono<Pagina<Asiento>> ejecutar(UUID recintoId, int numeroPagina, int size) {
        return asientoRepositoryPort.buscarPorRecintoId(recintoId)
                .collectList()
                .map(todos -> {
                    long total = todos.size();
                    List<Asiento> contenido = todos.stream()
                            .skip((long) numeroPagina * size)
                            .limit(size)
                            .toList();
                    return new Pagina<>(contenido, total, numeroPagina, size);
                });
    }
}
