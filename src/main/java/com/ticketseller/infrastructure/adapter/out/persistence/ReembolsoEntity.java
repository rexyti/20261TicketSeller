package com.ticketseller.infrastructure.adapter.out.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("reembolsos")
public class ReembolsoEntity {
    @Id
    private UUID id;
    @Column("ticket_id")
    private UUID ticketId;
    @Column("venta_id")
    private UUID ventaId;
    private BigDecimal monto;
    private String tipo;
    private String estado;
    @Column("fecha_solicitud")
    private LocalDateTime fechaSolicitud;
    @Column("fecha_completado")
    private LocalDateTime fechaCompletado;
    @Column("agente_id")
    private UUID agenteId;
}
