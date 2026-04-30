package com.ticketseller.infrastructure.adapter.in.rest.asiento.dto;

import com.ticketseller.domain.model.asiento.EstadoAsiento;
import java.time.Instant;

public record HistorialCambioResponse(
        Instant fechaHora,
        String usuarioId,
        EstadoAsiento estadoAnterior,
        EstadoAsiento estadoNuevo,
        String motivo
) {
}
