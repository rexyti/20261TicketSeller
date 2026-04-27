package com.ticketseller.infrastructure.adapter.in.rest.dto;

import java.util.UUID;

public record CortesiaResponse(
        UUID cortesiaId,
        String codigoUnico,
        String destinatario,
        String categoria,
        UUID asientoId,
        UUID ticketId,
        String estado
) {}
