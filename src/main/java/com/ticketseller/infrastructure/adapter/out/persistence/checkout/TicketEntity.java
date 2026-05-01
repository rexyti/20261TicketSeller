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
@Table("tickets")
public class TicketEntity {
    @Id
    private UUID id;
    @Column("venta_id")
    private UUID ventaId;
    @Column("evento_id")
    private UUID eventoId;
    @Column("zona_id")
    private UUID zonaId;
    @Column("compuerta_id")
    private UUID compuertaId;
    @Column("codigo_qr")
    private String codigoQr;
    private String estado;
    private BigDecimal precio;
    @Column("es_cortesia")
    private boolean esCortesia;
    @Column("asiento_id")
    private UUID asientoId;
    private String categoria;
    @Column("fecha_evento")
    private LocalDateTime fechaEvento;
    @Column("zona_nombre")
    private String zonaNombre;
    @Column("compuerta_nombre")
    private String compuertaNombre;
}
