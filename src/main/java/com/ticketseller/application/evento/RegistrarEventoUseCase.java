package com.ticketseller.application.evento;

import com.ticketseller.domain.exception.EventoSolapamientoException;
import com.ticketseller.domain.exception.RecintoNoDisponibleException;
import com.ticketseller.domain.model.EstadoEvento;
import com.ticketseller.domain.model.Evento;
import com.ticketseller.domain.model.Recinto;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class RegistrarEventoUseCase {

    private final EventoRepositoryPort eventoRepositoryPort;
    private final RecintoRepositoryPort recintoRepositoryPort;

    public Mono<Evento> ejecutar(Evento request) {
        return Mono.justOrEmpty(request)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("El request de evento es obligatorio")))
                .map(Evento::normalizarDatosRegistro)
                .doOnNext(Evento::validarDatosRegistro)
                .flatMap(this::validarRecintoDisponible)
                .flatMap(this::validarSolapamiento)
                .map(this::buildNuevoEvento)
                .flatMap(eventoRepositoryPort::guardar);
    }

    private Mono<Evento> validarRecintoDisponible(Evento evento) {
        return recintoRepositoryPort.buscarPorId(evento.getRecintoId())
                .filter(Recinto::isActivo)
                .switchIfEmpty(Mono.error(new RecintoNoDisponibleException(
                        "El recinto escogido para este evento no se encuentra disponible"
                )))
                .thenReturn(evento);
    }

    private Mono<Evento> validarSolapamiento(Evento evento) {
        return eventoRepositoryPort.buscarEventosSolapados(evento.getRecintoId(), evento.getFechaInicio(), evento.getFechaFin())
                .hasElements()
                .filter(existe -> !existe)
                .switchIfEmpty(Mono.error(new EventoSolapamientoException(
                        "Ya existe un evento en ese recinto para el rango de fechas indicado"
                )))
                .thenReturn(evento);
    }

    private Evento buildNuevoEvento(Evento request) {
        return request.toBuilder()
                .id(UUID.randomUUID())
                .estado(EstadoEvento.ACTIVO)
                .motivoCancelacion(null)
                .build();
    }
}

