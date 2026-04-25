package com.ticketseller.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class HistorialTicket {
    private UUID id;
    private UUID ticketId;
    private UUID agenteId;
    private EstadoTicket estadoAnterior;
    private EstadoTicket estadoNuevo;
    private LocalDateTime fecha;
    private String justificacion;
}
