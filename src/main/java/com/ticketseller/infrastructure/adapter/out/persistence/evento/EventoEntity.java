package com.ticketseller.infrastructure.adapter.out.persistence.evento;

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
@Table("eventos")
public class EventoEntity {
    @Id
    private UUID id;
    private String nombre;
    @Column("fecha_inicio")
    private LocalDateTime fechaInicio;
    @Column("fecha_fin")
    private LocalDateTime fechaFin;
    private String tipo;
    @Column("recinto_id")
    private UUID recintoId;
    private String estado;
}

