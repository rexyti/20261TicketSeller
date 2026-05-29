package com.ticketseller.infrastructure.adapter.out.persistence.bloqueos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Getter
@Setter
@Builder
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
    @Column("destinatario_id")
    private UUID destinatarioId;
    @Column("email_destinatario")
    private String emailDestinatario;
    private String categoria;
    @Column("ticket_id")
    private UUID ticketId;
    private String estado;
}
