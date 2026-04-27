package com.ticketseller.infrastructure.adapter.out.persistence.bloqueo;

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
@Table("bloqueos")
public class BloqueoEntity {
    @Id
    private UUID id;
    @Column("asiento_id")
    private UUID asientoId;
    @Column("evento_id")
    private UUID eventoId;
    private String destinatario;
    @Column("fecha_creacion")
    private LocalDateTime fechaCreacion;
    @Column("fecha_expiracion")
    private LocalDateTime fechaExpiracion;
    private String estado;
}
