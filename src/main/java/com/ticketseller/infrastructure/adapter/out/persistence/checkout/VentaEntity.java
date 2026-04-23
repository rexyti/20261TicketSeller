package com.ticketseller.infrastructure.adapter.out.persistence.checkout;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("ventas")
public class VentaEntity {
    @Id
    private UUID id;
    @Column("comprador_id")
    private UUID compradorId;
    @Column("evento_id")
    private UUID eventoId;
    private String estado;
    @Column("fecha_creacion")
    private LocalDateTime fechaCreacion;
    @Column("fecha_expiracion")
    private LocalDateTime fechaExpiracion;
    private BigDecimal total;
}

