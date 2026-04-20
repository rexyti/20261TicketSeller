package com.ticketseller.domain.model;

import com.ticketseller.domain.exception.RecintoInvalidoException;
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
public class Recinto {
    private UUID id;
    private String nombre;
    private String ciudad;
    private String direccion;
    private Integer capacidadMaxima;
    private String telefono;
    private LocalDateTime fechaCreacion;
    private Integer compuertasIngreso;
    private boolean activo;
    private CategoriaRecinto categoria;

    public void desactivar(){
        if (activo)
            activo = false;
    }

    public Recinto normalizarDatosRegistro() {
        return this.toBuilder()
                .nombre(trimOrNull(nombre))
                .ciudad(trimOrNull(ciudad))
                .direccion(trimOrNull(direccion))
                .telefono(trimOrNull(telefono))
                .build();
    }

    public void validarDatosRegistro() {
        validarTextoObligatorio(nombre, "nombre");
        validarTextoObligatorio(ciudad, "ciudad");
        validarTextoObligatorio(direccion, "direccion");
        validarTextoObligatorio(telefono, "telefono");
        validarPositivo(capacidadMaxima, "capacidadMaxima");
        validarPositivo(compuertasIngreso, "compuertasIngreso");
    }

    private void validarTextoObligatorio(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new RecintoInvalidoException("El campo " + campo + " es obligatorio");
        }
    }

    private void validarPositivo(Integer valor, String campo) {
        if (valor == null || valor < 1) {
            throw new RecintoInvalidoException("El campo " + campo + " debe ser mayor a cero");
        }
    }

    private String trimOrNull(String valor) {
        return valor == null ? null : valor.trim();
    }
}

