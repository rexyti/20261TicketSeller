package com.ticketseller.infrastructure.adapter.in.rest.dto;

import java.util.UUID;

public record DisponibilidadResponse(
        UUID asientoId,
        boolean disponible,
        String mensaje
) {}
