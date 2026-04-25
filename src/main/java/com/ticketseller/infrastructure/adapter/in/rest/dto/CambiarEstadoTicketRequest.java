package com.ticketseller.infrastructure.adapter.in.rest.dto;

import com.ticketseller.domain.model.EstadoTicket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CambiarEstadoTicketRequest(
    @NotNull(message = "El estado es obligatorio")
    EstadoTicket estado,
    
    @NotBlank(message = "La justificación es obligatoria")
    String justificacion
) {}
