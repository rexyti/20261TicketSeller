package com.ticketseller.infrastructure.adapter.out.persistence.asientohold;

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
@Table("asiento_holds")
public class AsientoHoldEntity {
    @Id
    private UUID id;
    @Column("asiento_id")
    private UUID asientoId;
    @Column("venta_id")
    private UUID ventaId;
    private String numero;
    @Column("expira_en")
    private LocalDateTime expiraEn;
    private String estado;
}
