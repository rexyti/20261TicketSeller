package com.ticketseller.infrastructure.adapter.out.persistence.compuerta;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("compuertas")
public class CompuertaEntity {
    @Id
    private UUID id;
    @Column("recinto_id")
    private UUID recintoId;
    @Column("zona_id")
    private UUID zonaId;
    private String nombre;
    @Column("es_general")
    private Boolean esGeneral;
}

