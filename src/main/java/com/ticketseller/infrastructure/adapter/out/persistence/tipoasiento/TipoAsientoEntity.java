package com.ticketseller.infrastructure.adapter.out.persistence.tipoasiento;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("tipos_asiento")
public class TipoAsientoEntity {
    @Id
    private UUID id;
    private String nombre;
    private String descripcion;
    private String estado;
    @Column("created_at")
    private OffsetDateTime createdAt;
}
