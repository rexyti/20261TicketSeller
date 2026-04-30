package com.ticketseller.infrastructure.adapter.out.persistence.promocion;

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
@Table("descuentos")
public class DescuentoEntity {
    @Id
    private UUID id;
    @Column("promocion_id")
    private UUID promocionId;
    private String tipo;
    private BigDecimal valor;
    @Column("zona_id")
    private UUID zonaId;
    private boolean acumulable;
}

