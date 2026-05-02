package com.ticketseller.application.bloqueos;

import java.time.LocalDateTime;
import java.util.UUID;

public record PanelItem(
        UUID id,
        TipoPanelItem tipo,
        UUID asientoId,
        String destinatario,
        String estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaExpiracion,
        String codigoUnico,
        String categoria
) {
}
