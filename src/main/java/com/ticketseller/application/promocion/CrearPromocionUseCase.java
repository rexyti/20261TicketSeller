package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.evento.EventoNotFoundException;
import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class CrearPromocionUseCase {

    private final PromocionRepositoryPort promocionRepositoryPort;
    private final EventoRepositoryPort eventoRepositoryPort;

    public Mono<Promocion> ejecutar(CrearPromocionCommand command) {
        return Mono.justOrEmpty(command)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("El command es obligatorio")))
                .doOnNext(this::validarFechas)
                .flatMap(this::validarEventoExiste)
                .map(this::buildPromocion)
                .map(Promocion::normalizarDatosRegistro)
                .doOnNext(Promocion::validarDatosRegistro)
                .flatMap(promocionRepositoryPort::guardar);
    }

    private void validarFechas(CrearPromocionCommand command) {
        if (command.fechaInicio() == null || command.fechaFin() == null) {
            throw new IllegalArgumentException("fechaInicio y fechaFin son obligatorias");
        }
        if (command.fechaFin().isBefore(command.fechaInicio())) {
            throw new IllegalArgumentException("fechaFin debe ser posterior o igual a fechaInicio");
        }
    }

    private Mono<CrearPromocionCommand> validarEventoExiste(CrearPromocionCommand command) {
        return eventoRepositoryPort.buscarPorId(command.eventoId())
                .switchIfEmpty(Mono.error(new EventoNotFoundException("Evento no encontrado")))
                .thenReturn(command);
    }

    private Promocion buildPromocion(CrearPromocionCommand command) {
        return Promocion.builder()
                .id(UUID.randomUUID())
                .nombre(command.nombre())
                .tipo(command.tipo())
                .eventoId(command.eventoId())
                .fechaInicio(command.fechaInicio())
                .fechaFin(command.fechaFin())
                .estado(EstadoPromocion.ACTIVA)
                .tipoUsuarioRestringido(command.tipoUsuarioRestringido())
                .build();
    }
}

