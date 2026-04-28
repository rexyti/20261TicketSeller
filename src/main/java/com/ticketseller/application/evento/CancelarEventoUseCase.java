package com.ticketseller.application.evento;

import com.ticketseller.domain.exception.evento.EventoNotFoundException;
import com.ticketseller.domain.model.evento.CancelacionEvento;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.repository.CancelacionEventoRepositoryPort;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class CancelarEventoUseCase {

    private final EventoRepositoryPort eventoRepositoryPort;
    private final CancelacionEventoRepositoryPort cancelacionEventoRepositoryPort;

    public Mono<Evento> ejecutar(UUID id, String motivo) {
        String motivoNormalizado = normalizarMotivo(motivo);
        return eventoRepositoryPort.buscarPorId(id)
                .switchIfEmpty(Mono.error(new EventoNotFoundException("Evento no encontrado")))
                .map(Evento::cancelar)
                .flatMap(eventoRepositoryPort::guardar)
                .flatMap(eventoCancelado -> guardarCancelacion(eventoCancelado, motivoNormalizado)
                        .thenReturn(eventoCancelado));
    }

    private Mono<CancelacionEvento> guardarCancelacion(Evento eventoCancelado, String motivoNormalizado) {
        return cancelacionEventoRepositoryPort.guardar(CancelacionEvento.builder()
                .id(UUID.randomUUID())
                .eventoId(eventoCancelado.getId())
                .fechaCancelacion(LocalDateTime.now())
                .motivo(motivoNormalizado)
                .build());
    }

    private String normalizarMotivo(String motivo) {
        String motivoNormalizado = motivo == null ? null : motivo.trim();
        if (motivoNormalizado == null || motivoNormalizado.isBlank()) {
            throw new IllegalArgumentException("El motivo de cancelacion es obligatorio");
        }
        return motivoNormalizado;
    }
}

