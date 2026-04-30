package com.ticketseller.domain.model.conciliacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Pago {
    private UUID id;
    private UUID ventaId;
    private String idExternoPasarela;
    private BigDecimal montoEsperado;
    private BigDecimal montoPasarela;
    private EstadoConciliacion estado;
    private UUID agenteId;
    private String justificacionResolucion;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public boolean hayDiscrepancia() {
        return montoDiferente();
    }

    private boolean montoDiferente(){
        return montoPasarela != null && montoEsperado.compareTo(montoPasarela) != 0;
    }
}
