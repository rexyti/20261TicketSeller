package com.ticketseller.infrastructure.adapter.out.persistence;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("historial_cambios_estado")
public class HistorialCambioEstadoEntity {
    
    @Id
    private UUID id;

    @Column("asiento_id")
    private UUID asientoId;

    @Column("evento_id")
    private UUID eventoId;

    @Column("usuario_id")
    private String usuarioId;

    @Column("estado_anterior")
    private String estadoAnterior;

    @Column("estado_nuevo")
    private String estadoNuevo;

    @Column("fecha_hora")
    private Instant fechaHora;

    @Column("motivo")
    private String motivo;
}
