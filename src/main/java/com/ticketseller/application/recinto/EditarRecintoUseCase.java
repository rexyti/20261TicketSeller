package com.ticketseller.application.recinto;

import com.ticketseller.domain.exception.RecintoConEventosException;
import com.ticketseller.domain.exception.RecintoNotFoundException;
import com.ticketseller.domain.model.Recinto;
import com.ticketseller.domain.port.out.RecintoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class EditarRecintoUseCase {

    private final RecintoRepositoryPort recintoRepositoryPort;

    public Mono<Recinto> ejecutar(UUID id, Recinto cambios) {
        return recintoRepositoryPort.buscarPorId(id)
                .switchIfEmpty(Mono.error(new RecintoNotFoundException("Recinto no encontrado")))
                .flatMap(actual -> aplicarCambios(actual, cambios));
    }

    private Mono<Recinto> aplicarCambios(Recinto actual, Recinto cambios) {
        validarCapacidadMaxima(actual, cambios);
        Mono<Boolean> tieneTickets = recintoTieneTicketsVendidos(actual, cambios.getCapacidadMaxima());

        return tieneTickets.flatMap(vendidos -> {
            if (vendidos) {
                return Mono.error(new RecintoConEventosException("No se puede cambiar la capacidad porque existen tickets vendidos"));
            }
            Recinto editado = buildRecintoEditado(actual, cambios);
            return recintoRepositoryPort.guardar(editado);
        });
    }

    private void validarCapacidadMaxima(Recinto actual, Recinto cambios) {
        cambios.setCapacidadMaxima(getNuevaCapacidad(actual.getCapacidadMaxima(), cambios.getCapacidadMaxima()));
    }

    private Integer getNuevaCapacidad(Integer capacidadActual, Integer nuevaCapacidad) {
        return nuevaCapacidad != null ? nuevaCapacidad : capacidadActual;
    }

    private Mono<Boolean> recintoTieneTicketsVendidos(Recinto actual, Integer nuevaCapacidad) {
        boolean cambiaCapacidad = !nuevaCapacidad.equals(actual.getCapacidadMaxima());
        return cambiaCapacidad
                ? recintoRepositoryPort.tieneTicketsVendidos(actual.getId())
                : Mono.just(false);
    }

    private Recinto buildRecintoEditado(Recinto actual, Recinto cambios) {
        return actual.toBuilder()
                .nombre(cambios.getNombre() != null ? cambios.getNombre() : actual.getNombre())
                .ciudad(cambios.getCiudad() != null ? cambios.getCiudad() : actual.getCiudad())
                .direccion(cambios.getDireccion() != null ? cambios.getDireccion() : actual.getDireccion())
                .telefono(cambios.getTelefono() != null ? cambios.getTelefono() : actual.getTelefono())
                .compuertasIngreso(cambios.getCompuertasIngreso() != null ? cambios.getCompuertasIngreso() : actual.getCompuertasIngreso())
                .capacidadMaxima(cambios.getCapacidadMaxima())
                .build();
    }
}

