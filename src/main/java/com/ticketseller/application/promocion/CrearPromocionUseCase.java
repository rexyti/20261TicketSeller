package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.evento.EventoNotFoundException;
import com.ticketseller.domain.exception.promocion.FechasInvalidasPromocionException;
import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class CrearPromocionUseCase {

    private final PromocionRepositoryPort promocionRepositoryPort;
    private final EventoRepositoryPort eventoRepositoryPort;

    public Mono<Promocion> ejecutar(Promocion request) {
        return Mono.just(request)
                .doOnNext(this::validarFechas)
                .flatMap(this::validarEventoExiste)
                .map(this::buildNuevaPromocion)
                .flatMap(promocionRepositoryPort::guardar);
    }

    private void validarFechas(Promocion p) {
        if (sinFechasEstablecidas(p)) {
            throw new FechasInvalidasPromocionException("Las fechas de inicio y fin son obligatorias");
        }
        if (fechaInicioPosteriorFechaFin(p)) {
            throw new FechasInvalidasPromocionException("La fecha de inicio debe ser anterior a la fecha de fin");
        }
    }

    private boolean sinFechasEstablecidas(Promocion p){
        return p.getFechaInicio() == null || p.getFechaFin() == null;
    }

    private boolean fechaInicioPosteriorFechaFin(Promocion p) {
        return p.getFechaInicio().isAfter(p.getFechaFin());
    }

    private Mono<Promocion> validarEventoExiste(Promocion p) {
        return eventoRepositoryPort.buscarPorId(p.getEventoId())
                .switchIfEmpty(Mono.error(new EventoNotFoundException("El evento indicado no existe")))
                .thenReturn(p);
    }

    private Promocion buildNuevaPromocion(Promocion request) {
        return request.toBuilder()
                .estado(EstadoPromocion.ACTIVA)
                .build();
    }
}
