package com.ticketseller.application.recinto;

import com.ticketseller.domain.exception.RecintoConEventosException;
import com.ticketseller.domain.exception.RecintoNotFoundException;
import com.ticketseller.domain.model.Recinto;
import com.ticketseller.domain.port.out.RecintoRepositoryPort;
import reactor.core.publisher.Mono;

import java.util.UUID;

public class EditarRecintoUseCase {

    private final RecintoRepositoryPort recintoRepositoryPort;

    public EditarRecintoUseCase(RecintoRepositoryPort recintoRepositoryPort) {
        this.recintoRepositoryPort = recintoRepositoryPort;
    }

    public Mono<Recinto> ejecutar(UUID id, Recinto cambios) {
        return recintoRepositoryPort.buscarPorId(id)
                .switchIfEmpty(Mono.error(new RecintoNotFoundException("Recinto no encontrado")))
                .flatMap(actual -> aplicarCambios(actual, cambios));
    }

    private Mono<Recinto> aplicarCambios(Recinto actual, Recinto cambios) {
        Integer nuevaCapacidad = cambios.getCapacidadMaxima() != null ? cambios.getCapacidadMaxima() : actual.getCapacidadMaxima();
        boolean cambiaCapacidad = !nuevaCapacidad.equals(actual.getCapacidadMaxima());
        Mono<Boolean> tieneTickets = cambiaCapacidad
                ? recintoRepositoryPort.tieneTicketsVendidos(actual.getId())
                : Mono.just(false);

        return tieneTickets.flatMap(vendidos -> {
            if (vendidos) {
                return Mono.error(new RecintoConEventosException("No se puede cambiar la capacidad porque existen tickets vendidos"));
            }
            Recinto editado = actual.toBuilder()
                    .nombre(cambios.getNombre() != null ? cambios.getNombre() : actual.getNombre())
                    .ciudad(cambios.getCiudad() != null ? cambios.getCiudad() : actual.getCiudad())
                    .direccion(cambios.getDireccion() != null ? cambios.getDireccion() : actual.getDireccion())
                    .telefono(cambios.getTelefono() != null ? cambios.getTelefono() : actual.getTelefono())
                    .compuertasIngreso(cambios.getCompuertasIngreso() != null ? cambios.getCompuertasIngreso() : actual.getCompuertasIngreso())
                    .capacidadMaxima(nuevaCapacidad)
                    .build();
            return recintoRepositoryPort.guardar(editado);
        });
    }
}

