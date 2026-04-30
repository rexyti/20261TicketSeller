package com.ticketseller.domain.model.ticket;

import com.ticketseller.domain.exception.postventa.TransicionEstadoInvalidaException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {
    private UUID id;
    private UUID ventaId;
    private UUID eventoId;
    private UUID zonaId;
    private UUID compuertaId;
    private String codigoQr;
    private EstadoTicket estado;
    private BigDecimal precio;
    private boolean esCortesia;
    private UUID asientoId;
    private AccessDetails accessDetails;

    public void validarTransicionA(EstadoTicket destino) {
        if (transicionInvalida(destino)) {
            throw new TransicionEstadoInvalidaException(estado, destino);
        }
    }

    private boolean transicionInvalida(EstadoTicket destino) {
        return estado.equals(destino) || !estado.transicionesPermitidas().contains(destino);
    }

    public Ticket normalizarDatosRegistro() {
        return toBuilder()
                .codigoQr(trimOrNull(codigoQr))
                .build();
    }

    public void validarDatosRegistro() {
        validarObligatorio(id, "id");
        validarObligatorio(ventaId, "ventaId");
        validarObligatorio(eventoId, "eventoId");
        validarObligatorio(zonaId, "zonaId");
        validarObligatorio(precio, "precio");
        if (isPrecioInvalido()) {
            throw new IllegalArgumentException("precio debe ser mayor o igual a 0");
        }
        if (ticketVendidoSinQrGenerado()) {
            throw new IllegalArgumentException("códigoQr es obligatorio para tickets vendidos");
        }
        accessDetails.validar();
    }

    private void validarObligatorio(Object valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException("El campo %s es obligatorio".formatted(campo));
        }
    }

    private String trimOrNull(String valor) {
        return valor == null ? null : valor.trim();
    }

    private boolean isPrecioInvalido(){
        return precio.compareTo(BigDecimal.ZERO) < 0;
    }

    private boolean ticketVendidoSinQrGenerado(){
        return EstadoTicket.VENDIDO.equals(estado) && (codigoQr == null || codigoQr.isBlank());
    }
}
