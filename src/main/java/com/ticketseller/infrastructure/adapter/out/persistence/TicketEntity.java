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
@Table("tickets")
public class TicketEntity {
    @Id
    private UUID id;
    private UUID ventaId;
    private UUID eventoId;
    private UUID zonaId;
    private UUID compuertaId;
    private String codigoQr;
    private String estado;
    private BigDecimal precio;
    private boolean esCortesia;
    private LocalDateTime createdAt;
}
