package com.ticketseller.infrastructure.adapter.in.rest.postventa.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record CancelarTicketRequest(
        @NotEmpty(message = "ticketIds es obligatorio")
        List<UUID> ticketIds
) {
}

