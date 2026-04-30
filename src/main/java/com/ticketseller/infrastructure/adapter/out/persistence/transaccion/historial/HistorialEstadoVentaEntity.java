package com.ticketseller.infrastructure.adapter.out.persistence.transaccion.historial;

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
@Table("historial_estado_venta")
public class HistorialEstadoVentaEntity {
    @Id
    private UUID id;
    @Column("venta_id")
    private UUID ventaId;
    @Column("actor_id")
    private UUID actorId;
    @Column("estado_anterior")
    private String estadoAnterior;
    @Column("estado_nuevo")
    private String estadoNuevo;
    private String justificacion;
    @Column("fecha_cambio")
    private LocalDateTime fechaCambio;
}
