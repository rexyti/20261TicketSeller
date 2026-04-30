package com.ticketseller.infrastructure.adapter.in.rest.conciliacion.dto;

import com.ticketseller.domain.model.conciliacion.EstadoConciliacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PagoResponse(
        UUID id,
        UUID ventaId,
        String idExternoPasarela,
        BigDecimal montoEsperado,
        BigDecimal montoPasarela,
        EstadoConciliacion estado,
        UUID agenteId,
        String justificacionResolucion,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}
