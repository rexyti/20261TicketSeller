package com.ticketseller.infrastructure.adapter.out.persistence.promocion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("codigos_promocionales")
public class CodigoPromocionalEntity {

    @Id
    private UUID id;
    private String codigo;
    @Column("promocion_id")
    private UUID promocionId;
    @Column("usos_maximos")
    private Integer usosMaximos;
    @Column("usos_actuales")
    private int usosActuales;
    @Column("fecha_inicio")
    private LocalDateTime fechaInicio;
    @Column("fecha_fin")
    private LocalDateTime fechaFin;
    private String estado;
}
