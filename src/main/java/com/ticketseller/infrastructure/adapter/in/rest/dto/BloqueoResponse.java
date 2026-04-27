package com.ticketseller.infrastructure.adapter.in.rest.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BloqueoResponse(
        UUID bloqueoId,
        UUID asientoId,
        UUID eventoId,
        String destinatario,
        String estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaExpiracion
) {}
