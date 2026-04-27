package com.ticketseller.infrastructure.adapter.out.persistence.cortesia;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("cortesias")
public class CortesiaEntity {
    @Id
    private UUID id;
    @Column("asiento_id")
    private UUID asientoId;
    @Column("evento_id")
    private UUID eventoId;
    private String destinatario;
    private String categoria;
    @Column("codigo_unico")
    private String codigoUnico;
    @Column("ticket_id")
    private UUID ticketId;
    private String estado;
}
