package com.ticketseller.infrastructure.adapter.out.persistence;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("ventas")
public class VentaEntity {
    @Id
    private UUID id;
    private UUID compradorId;
    private UUID eventoId;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaExpiracion;
    private BigDecimal total;
}
