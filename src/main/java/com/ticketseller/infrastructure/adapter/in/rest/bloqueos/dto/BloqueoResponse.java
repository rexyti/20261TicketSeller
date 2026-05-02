package com.ticketseller.infrastructure.adapter.in.rest.bloqueos.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BloqueoResponse(
        UUID bloqueoId,
        List<UUID> asientoIds,
        String destinatario,
        String estado,
        LocalDateTime fechaCreacion
) {
}
