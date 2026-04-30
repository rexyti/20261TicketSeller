package com.ticketseller.infrastructure.adapter.in.rest.transaccion.dto;

import com.ticketseller.domain.model.venta.EstadoVenta;

import java.time.LocalDateTime;
import java.util.UUID;

public record HistorialEstadoVentaResponse(
        UUID id,
        UUID ventaId,
        UUID actorId,
        EstadoVenta estadoAnterior,
        EstadoVenta estadoNuevo,
        String justificacion,
        LocalDateTime fechaCambio
) {
}
