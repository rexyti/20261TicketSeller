package com.ticketseller.application.evento;

import com.ticketseller.domain.exception.evento.EventoNotFoundException;
import com.ticketseller.domain.exception.evento.EventoSolapamientoException;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class EditarEventoUseCase {

    private final EventoRepositoryPort eventoRepositoryPort;

    public Mono<Evento> ejecutar(UUID id, Evento cambios) {
        return eventoRepositoryPort.buscarPorId(id)
                .switchIfEmpty(Mono.error(new EventoNotFoundException("Evento no encontrado")))
                .flatMap(actual -> {
                    actual.validarEditable();
                    Evento editado = buildEventoEditado(actual, cambios).normalizarDatosRegistro();
                    editado.validarDatosRegistro();
                    return validarSolapamiento(editado)
                            .then(eventoRepositoryPort.guardar(editado));
                });
    }

    private Mono<Void> validarSolapamiento(Evento evento) {
        return eventoRepositoryPort.buscarEventosSolapados(evento.getRecintoId(), evento.getFechaInicio(), evento.getFechaFin())
                .filter(existing -> !existing.getId().equals(evento.getId()))
                .hasElements()
                .filter(existe -> !existe)
                .switchIfEmpty(Mono.error(new EventoSolapamientoException(
                        "Ya existe un evento en ese recinto para el rango de fechas indicado"
                )))
                .then();
    }

    private Evento buildEventoEditado(Evento actual, Evento cambios) {
        return actual.toBuilder()
                .nombre(cambios.getNombre() != null ? cambios.getNombre() : actual.getNombre())
                .fechaInicio(cambios.getFechaInicio() != null ? cambios.getFechaInicio() : actual.getFechaInicio())
                .fechaFin(cambios.getFechaFin() != null ? cambios.getFechaFin() : actual.getFechaFin())
                .tipo(cambios.getTipo() != null ? cambios.getTipo() : actual.getTipo())
                .recintoId(cambios.getRecintoId() != null ? cambios.getRecintoId() : actual.getRecintoId())
                .build();
    }
}

