package com.ticketseller.infrastructure.adapter.in.rest.bloqueos.dto;

import java.util.UUID;

public record CortesiaResponse(
        UUID cortesiaId,
        UUID destinatarioId,
        String emailDestinatario,
        String categoria,
        UUID asientoId,
        UUID ticketId
) {
}
