package com.ticketseller.application.transaccion;

import com.ticketseller.domain.model.venta.EstadoVenta;

import java.time.LocalDateTime;
import java.util.UUID;

public record FiltroTransacciones(
        EstadoVenta estado,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        UUID eventoId
) {
}
