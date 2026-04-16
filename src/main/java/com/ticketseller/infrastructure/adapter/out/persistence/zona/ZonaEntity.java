package com.ticketseller.infrastructure.adapter.out.persistence.zona;

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
@Table("zonas")
public class ZonaEntity {
    @Id
    private UUID id;
    @Column("recinto_id")
    private UUID recintoId;
    private String nombre;
    private Integer capacidad;
}

