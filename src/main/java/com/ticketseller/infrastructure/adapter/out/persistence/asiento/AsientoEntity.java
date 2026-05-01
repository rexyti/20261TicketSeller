package com.ticketseller.infrastructure.adapter.out.persistence.asiento;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("asientos")
public class AsientoEntity {
    @Id
    private UUID id;
    private String fila;
    private int columna;
    private String numero;
    @Column("zona_id")
    private UUID zonaId;
    private String tipo;
    private String estado;
    @Version
    private Long version;
    @Column("expira_en")
    private LocalDateTime expiraEn;
}
