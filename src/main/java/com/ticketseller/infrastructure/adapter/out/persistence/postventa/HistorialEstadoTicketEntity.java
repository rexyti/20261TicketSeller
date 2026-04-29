package com.ticketseller.infrastructure.adapter.out.persistence.postventa;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("historial_estado_ticket")
public class HistorialEstadoTicketEntity {
    @Id
    private UUID id;
    @Column("ticket_id")
    private UUID ticketId;
    @Column("agente_id")
    private UUID agenteId;
    @Column("estado_anterior")
    private String estadoAnterior;
    @Column("estado_nuevo")
    private String estadoNuevo;
    private String justificacion;
    @Column("fecha_cambio")
    private LocalDateTime fechaCambio;
}

