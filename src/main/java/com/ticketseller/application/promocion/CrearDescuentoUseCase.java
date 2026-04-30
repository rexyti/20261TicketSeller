package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.promocion.PromocionNoActivaException;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.promocion.Descuento;
import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.domain.repository.DescuentoRepositoryPort;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class CrearDescuentoUseCase {

    private final DescuentoRepositoryPort descuentoRepositoryPort;
    private final PromocionRepositoryPort promocionRepositoryPort;
    private final ZonaRepositoryPort zonaRepositoryPort;
    private final EventoRepositoryPort eventoRepositoryPort;

    public Mono<Descuento> ejecutar(CrearDescuentoCommand command) {
        return Mono.justOrEmpty(command)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("El command es obligatorio")))
                .flatMap(this::validarPromocionActiva)
                .flatMap(this::validarZonaPerteneceAlEvento)
                .map(this::buildDescuento)
                .doOnNext(Descuento::validarDatosRegistro)
                .flatMap(descuentoRepositoryPort::guardar);
    }

    private Mono<CrearDescuentoContexto> validarPromocionActiva(CrearDescuentoCommand command) {
        return promocionRepositoryPort.buscarPorId(command.promocionId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Promocion no encontrada")))
                .filter(promocion -> EstadoPromocion.ACTIVA.equals(promocion.getEstado()))
                .switchIfEmpty(Mono.error(new PromocionNoActivaException("La promocion debe estar activa")))
                .map(promocion -> new CrearDescuentoContexto(command, promocion));
    }

    private Mono<CrearDescuentoContexto> validarZonaPerteneceAlEvento(CrearDescuentoContexto contexto) {
        if (contexto.command().zonaId() == null) {
            return Mono.just(contexto);
        }
        Mono<Evento> eventoMono = eventoRepositoryPort.buscarPorId(contexto.promocion().getEventoId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Evento no encontrado para la promocion")));
        Mono<Zona> zonaMono = zonaRepositoryPort.buscarPorId(contexto.command().zonaId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Zona no encontrada")));

        return Mono.zip(eventoMono, zonaMono)
                .filter(tuple -> tuple.getT2().getRecintoId().equals(tuple.getT1().getRecintoId()))
                .switchIfEmpty(Mono.error(new IllegalArgumentException("La zona no pertenece al evento de la promocion")))
                .thenReturn(contexto);
    }

    private Descuento buildDescuento(CrearDescuentoContexto contexto) {
        return Descuento.builder()
                .id(UUID.randomUUID())
                .promocionId(contexto.command().promocionId())
                .tipo(contexto.command().tipo())
                .valor(contexto.command().valor())
                .zonaId(contexto.command().zonaId())
                .acumulable(contexto.command().acumulable())
                .build();
    }

    private record CrearDescuentoContexto(CrearDescuentoCommand command, Promocion promocion) {
    }
}

