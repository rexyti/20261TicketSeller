package com.ticketseller.domain.model;

import com.ticketseller.domain.exception.EventoEnProgresoException;
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
public class Evento {
    private UUID id;
    private String nombre;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String tipo;
    private UUID recintoId;
    private EstadoEvento estado;
    private String motivoCancelacion;

    public Evento normalizarDatosRegistro() {
        return toBuilder()
                .nombre(trimOrNull(nombre))
                .tipo(trimOrNull(tipo))
                .motivoCancelacion(trimOrNull(motivoCancelacion))
                .build();
    }

    public void validarDatosRegistro() {
        validarTextoObligatorio(nombre, "nombre");
        validarTextoObligatorio(tipo, "tipo");
        validarObligatorio(recintoId, "recintoId");
        validarObligatorio(fechaInicio, "fechaInicio");
        validarObligatorio(fechaFin, "fechaFin");
        if (fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("fechaFin debe ser posterior a fechaInicio");
        }
    }

    public void validarEditable() {
        if (EstadoEvento.EN_PROGRESO.equals(estado)) {
            throw new EventoEnProgresoException("No se puede editar un evento en progreso");
        }
    }

    public Evento cancelarConMotivo(String motivo) {
        String motivoNormalizado = trimOrNull(motivo);
        if (motivoNormalizado == null || motivoNormalizado.isBlank()) {
            throw new IllegalArgumentException("El motivo de cancelacion es obligatorio");
        }
        return toBuilder()
                .estado(EstadoEvento.CANCELADO)
                .motivoCancelacion(motivoNormalizado)
                .build();
    }

    private void validarTextoObligatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo %s es obligatorio".formatted(campo));
        }
    }

    private void validarObligatorio(Object valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException("El campo %s es obligatorio".formatted(campo));
        }
    }

    private String trimOrNull(String valor) {
        return valor == null ? null : valor.trim();
    }
}


