package com.ticketseller.infrastructure.adapter.out.persistence.preciozona;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("precios_zona")
public class PrecioZonaEntity {
    @Id
    private UUID id;
    @Column("evento_id")
    private UUID eventoId;
    @Column("zona_id")
    private UUID zonaId;
    private BigDecimal precio;
}

