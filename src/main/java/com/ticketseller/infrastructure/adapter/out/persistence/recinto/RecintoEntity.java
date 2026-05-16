package com.ticketseller.infrastructure.adapter.out.persistence.recinto;

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
@Table("recintos")
public class RecintoEntity {
    @Id
    private UUID id;
    private String nombre;
    private String ciudad;
    private String direccion;
    @Column("capacidad_maxima")
    private Integer capacidadMaxima;
    private String telefono;
    @Column("fecha_creacion")
    private LocalDateTime fechaCreacion;
    @Column("compuertas_ingreso")
    private Integer compuertasIngreso;
    private Boolean activo;
    private String categoria;
}

