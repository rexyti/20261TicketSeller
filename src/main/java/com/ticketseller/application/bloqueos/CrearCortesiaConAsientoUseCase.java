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

    public Mono<Cortesia> ejecutar(UUID eventoId, UUID destinatarioId, String emailDestinatario,
                                   CategoriaCortesia categoria, UUID asientoId) {
        return asientoRepositoryPort.buscarPorId(asientoId)
                .switchIfEmpty(Mono.error(new AsientoNotFoundException(
                        "Asiento %s no encontrado".formatted(asientoId))))
                .flatMap(asiento -> validarDisponibleParaEvento(asiento, eventoId))
                .flatMap(asiento -> crearBloqueo(asiento, eventoId, destinatarioId, emailDestinatario)
                        .then(cortesiaTicketCreator.crear(eventoId, destinatarioId, emailDestinatario,
                                categoria, asiento.getZonaId(), asiento)));
    }

    private Mono<Asiento> validarDisponibleParaEvento(Asiento asiento, UUID eventoId) {
        if (!asiento.isDisponible()) {
            return Mono.error(new AsientoOcupadoException(asiento.getId()));
        }
        return Mono.zip(
                bloqueoRepositoryPort.buscarActivoPorAsientoYEvento(asiento.getId(), eventoId).hasElement(),
                asientoHoldRepositoryPort.buscarActivoPorAsientoYEvento(asiento.getId(), eventoId).hasElement()
        ).flatMap(tuple -> Mono.just(asiento)
                .filter(a -> !tuple.getT1())
                .switchIfEmpty(Mono.error(new AsientoYaBloqueadoException(asiento.getId())))
                .filter(a -> !tuple.getT2())
                .switchIfEmpty(Mono.error(new AsientoOcupadoException(asiento.getId()))));
    }

    private Mono<Bloqueo> crearBloqueo(Asiento asiento, UUID eventoId,
                                       UUID destinatarioId, String emailDestinatario) {
        String destinatarioLabel = getDestinatarioLabel(emailDestinatario, destinatarioId);
        Bloqueo bloqueo = Bloqueo.builder()
                .asientoId(asiento.getId())
                .eventoId(eventoId)
                .destinatario(destinatarioLabel)
                .fechaCreacion(LocalDateTime.now())
                .fechaExpiracion(null)
                .estado(EstadoBloqueo.ACTIVO)
                .build();
        return bloqueoRepositoryPort.guardar(bloqueo);
    }

    private String getDestinatarioLabel(String emailDestinatario, UUID destinatarioId) {
        return emailDestinatario != null ? emailDestinatario : destinatarioId.toString();
    }
}
