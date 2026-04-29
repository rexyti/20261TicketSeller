package com.ticketseller.infrastructure.adapter.in.rest.dto.postventa;

import com.ticketseller.domain.model.ticket.EstadoTicket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CambiarEstadoTicketRequest(
        @NotNull(message = "estado es obligatorio")
        EstadoTicket estado,
        @NotBlank(message = "justificacion es obligatoria")
        String justificacion,
        UUID agenteId
) {
}

