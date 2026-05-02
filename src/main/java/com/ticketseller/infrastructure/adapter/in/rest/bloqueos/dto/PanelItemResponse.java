package com.ticketseller.infrastructure.adapter.in.rest.bloqueos.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record PanelItemResponse(
        UUID id,
        String tipo,
        UUID asientoId,
        String destinatario,
        String estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaExpiracion,
        String codigoUnico,
        String categoria
) {
}
