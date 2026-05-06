package com.ticketseller.domain.model.asiento;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AsientoHold {
    private UUID id;
    private UUID asientoId;
    private UUID ventaId;
    private String numero;
    private LocalDateTime expiraEn;
    private EstadoHold estado;
}
