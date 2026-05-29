package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.exception.asiento.AsientoNotFoundException;
import com.ticketseller.domain.exception.bloqueos.AsientoOcupadoException;
import com.ticketseller.domain.exception.bloqueos.AsientoYaBloqueadoException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.bloqueos.Bloqueo;
import com.ticketseller.domain.model.bloqueos.CategoriaCortesia;
import com.ticketseller.domain.model.bloqueos.Cortesia;
import com.ticketseller.domain.model.bloqueos.EstadoBloqueo;
import com.ticketseller.domain.repository.AsientoHoldRepositoryPort;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.BloqueoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class CrearCortesiaConAsientoUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;
    private final BloqueoRepositoryPort bloqueoRepositoryPort;
    private final AsientoHoldRepositoryPort asientoHoldRepositoryPort;
    private final CortesiaTicketCreator cortesiaTicketCreator;

    public Mono<Cortesia> ejecutar(UUID eventoId, String destinatario, CategoriaCortesia categoria, UUID asientoId) {
        return asientoRepositoryPort.buscarPorId(asientoId)
                .switchIfEmpty(Mono.error(new AsientoNotFoundException(
                        "Asiento %s no encontrado".formatted(asientoId))))
                .flatMap(asiento -> validarDisponibleParaEvento(asiento, eventoId))
                .flatMap(asiento -> crearBloqueo(asiento, eventoId, destinatario)
                        .then(cortesiaTicketCreator.crear(eventoId, destinatario, categoria, asiento.getZonaId(), asiento)));
    }

    private Mono<Asiento> validarDisponibleParaEvento(Asiento asiento, UUID eventoId) {
        if (!asiento.isDisponible()) {
            return Mono.error(new AsientoOcupadoException(asiento.getId()));
        }
        return Mono.zip(
                bloqueoRepositoryPort.buscarActivoPorAsientoYEvento(asiento.getId(), eventoId).hasElement(),
                asientoHoldRepositoryPort.buscarActivoPorAsientoYEvento(asiento.getId(), eventoId).hasElement()
        ).flatMap(tuple -> {
            if (tuple.getT1()) {
                return Mono.error(new AsientoYaBloqueadoException(asiento.getId()));
            }
            if (tuple.getT2()) {
                return Mono.error(new AsientoOcupadoException(asiento.getId()));
            }
            return Mono.just(asiento);
        });
    }

    private Mono<Bloqueo> crearBloqueo(Asiento asiento, UUID eventoId, String destinatario) {
        Bloqueo bloqueo = Bloqueo.builder()
                .asientoId(asiento.getId())
                .eventoId(eventoId)
                .destinatario(destinatario)
                .fechaCreacion(LocalDateTime.now())
                .fechaExpiracion(null)
                .estado(EstadoBloqueo.ACTIVO)
                .build();
        return bloqueoRepositoryPort.guardar(bloqueo);
    }
}
