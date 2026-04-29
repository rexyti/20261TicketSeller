package com.ticketseller.domain.model.asiento;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Asiento {
    private UUID id;
    private String fila;
    private Integer columna;
    private String numero;
    private UUID zonaId;
    private TipoAsiento tipoAsiento;
    private EstadoAsiento estado;

    public Asiento normalizarDatosRegistro() {
        return this.toBuilder()
                .fila(trimOrNull(fila))
                .numero(fila + columna)
                .build();
    }

    public void validarDatosRegistro() {
        validarObligatorio(id, "id");
        validarObligatorio(zonaId, "zonaId");
        validarObligatorio(fila, "fila");
        validarObligatorio(columna, "columna");
        validarObligatorio(numero, "numero");
        validarObligatorio(estado, "estado");
    }

    private void validarObligatorio(Object valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException("El campo %s es obligatorio".formatted(campo));
        }
    }

    private String trimOrNull(String valor) {
        return valor == null ? null : valor.trim();
    }

    public boolean esTransicionPermitida(EstadoAsiento nuevoEstado){
        return transicionPermitida(this.estado, nuevoEstado);
    }

    private boolean transicionPermitida(EstadoAsiento actual, EstadoAsiento nuevo) {
        validarEstados(actual, nuevo);
        return switch (actual){
            case DISPONIBLE -> validoFromDisponible(nuevo);
            case BLOQUEADO -> validoFromBloqueado(nuevo);
            case RESERVADO -> validoFromReservado(nuevo);
            case VENDIDO -> validoFromVendido(nuevo);
            case MANTENIMIENTO -> validoFromMantenimiento(nuevo);
            default -> false;
        };
    }

    private void validarEstados(EstadoAsiento actual, EstadoAsiento nuevo) {
        if (actual == null || nuevo == null) {
            throw new IllegalArgumentException("Los estados no pueden ser nulos");
        }
        if (actual.equals(nuevo)) {
            throw new IllegalArgumentException("El nuevo estado debe ser diferente al actual");
        }
    }

    private boolean validoFromMantenimiento(EstadoAsiento nuevo) {
        return EstadoAsiento.DISPONIBLE.equals(nuevo) || EstadoAsiento.BLOQUEADO.equals(nuevo);
    }

    private boolean validoFromVendido(EstadoAsiento nuevo) {
        return EstadoAsiento.ANULADO.equals(nuevo);
    }

    private boolean validoFromReservado(EstadoAsiento nuevo) {
        return EstadoAsiento.DISPONIBLE.equals(nuevo) || EstadoAsiento.VENDIDO.equals(nuevo);
    }

    private boolean validoFromBloqueado(EstadoAsiento nuevo) {
        return EstadoAsiento.DISPONIBLE.equals(nuevo) || EstadoAsiento.MANTENIMIENTO.equals(nuevo);
    }

    private boolean validoFromDisponible(EstadoAsiento nuevo) {
        return EstadoAsiento.RESERVADO.equals(nuevo) || EstadoAsiento.BLOQUEADO.equals(nuevo)
                || EstadoAsiento.MANTENIMIENTO.equals(nuevo);
    }
}
