package com.ticketseller.domain.model.postventa;

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
public class Reembolso {
    private UUID id;
    private UUID ticketId;
    private UUID ventaId;
    private BigDecimal monto;
    private TipoReembolso tipo;
    private EstadoReembolso estado;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaCompletado;
    private UUID agenteId;

    public void validarDatosRegistro() {
        validarObligatorio(id, "id");
        validarObligatorio(ticketId, "ticketId");
        validarObligatorio(ventaId, "ventaId");
        validarObligatorio(monto, "monto");
        validarObligatorio(tipo, "tipo");
        validarObligatorio(estado, "estado");
        validarObligatorio(fechaSolicitud, "fechaSolicitud");
        if (monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("monto debe ser mayor a 0");
        }
    }

    private void validarObligatorio(Object valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException("El campo %s es obligatorio".formatted(campo));
        }
    }
}

