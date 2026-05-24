package com.ticketseller.domain.model.promocion;

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
public class Promocion {

    private UUID id;
    private String nombre;
    private TipoPromocion tipo;
    private UUID eventoId;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private EstadoPromocion estado;
    private TipoUsuario tipoUsuarioRestringido;

    public boolean isActiva() {
        return EstadoPromocion.ACTIVA.equals(estado);
    }

    public boolean estaVigenteEn(LocalDateTime momento) {
        return momento.isAfter(fechaInicio) && momento.isBefore(fechaFin);
    }

    public void validar() {
        if (nombreInvalido()) {
            throw new IllegalArgumentException("El nombre de la promoción no puede ser nulo ni vacío");
        }
    }

    private boolean nombreInvalido() {
        return nombre == null || nombre.isBlank();
    }

    public boolean isPausada(){
        return EstadoPromocion.PAUSADA.equals(estado);
    }

    public boolean isFinalizada(){
        return EstadoPromocion.FINALIZADA.equals(estado);
    }

    public void pausar(){
        if (isActiva()){
            this.estado = EstadoPromocion.PAUSADA;
        }
    }

    public void finalizar(){
        if (isFinalizable()){
            this.estado = EstadoPromocion.FINALIZADA;
        }
    }

    private boolean isFinalizable(){
        return !isFinalizada();
    }
}
