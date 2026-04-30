package com.ticketseller.infrastructure.adapter.out.persistence.promocion;

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
@Table("promociones")
public class PromocionEntity {
    @Id
    private UUID id;
    private String nombre;
    private String tipo;
    @Column("evento_id")
    private UUID eventoId;
    @Column("fecha_inicio")
    private LocalDateTime fechaInicio;
    @Column("fecha_fin")
    private LocalDateTime fechaFin;
    private String estado;
    @Column("tipo_usuario_restringido")
    private String tipoUsuarioRestringido;
}

