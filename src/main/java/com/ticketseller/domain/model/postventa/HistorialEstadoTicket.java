package com.ticketseller.domain.model.postventa;

import com.ticketseller.domain.model.ticket.EstadoTicket;
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
public class HistorialEstadoTicket {
    private UUID id;
    private UUID ticketId;
    private UUID agenteId;
    private EstadoTicket estadoAnterior;
    private EstadoTicket estadoNuevo;
    private String justificacion;
    private LocalDateTime fechaCambio;
}

