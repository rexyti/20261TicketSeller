package com.ticketseller.infrastructure.adapter.out.persistence.cancelacionevento;

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
@Table("cancelaciones_evento")
public class CancelacionEventoEntity {
    @Id
    private UUID id;
    @Column("evento_id")
    private UUID eventoId;
    @Column("fecha_cancelacion")
    private LocalDateTime fechaCancelacion;
    private String motivo;
}

