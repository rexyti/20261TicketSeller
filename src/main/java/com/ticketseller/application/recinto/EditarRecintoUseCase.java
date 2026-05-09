package com.ticketseller.application.recinto;

import com.ticketseller.domain.exception.recinto.RecintoConEventosException;
import com.ticketseller.domain.exception.recinto.RecintoDuplicadoException;
import com.ticketseller.domain.exception.recinto.RecintoNotFoundException;
import com.ticketseller.domain.model.recinto.Recinto;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
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

        return validarNombreYCiudadUnicos(actual, cambios)
                .then(tieneTickets.filter(tiene -> !tiene)
                        .switchIfEmpty(Mono.error(new RecintoConEventosException("No se puede cambiar la capacidad máxima porque hay tickets vendidos")))
                        .map(permitido -> buildRecintoEditado(actual, cambios))
                        .flatMap(recintoRepositoryPort::guardar));
    }

    private Mono<Void> validarNombreYCiudadUnicos(Recinto actual, Recinto cambios) {
        String nuevoNombre = cambios.getNombre() != null ? cambios.getNombre() : actual.getNombre();
        String nuevaCiudad = cambios.getCiudad() != null ? cambios.getCiudad() : actual.getCiudad();
        boolean cambioNombreOCiudad = !nuevoNombre.equals(actual.getNombre()) || !nuevaCiudad.equals(actual.getCiudad());
        if (!cambioNombreOCiudad) {
            return Mono.empty();
        }
        return recintoRepositoryPort.buscarPorNombreYCiudad(nuevoNombre, nuevaCiudad)
                .flatMap(existente -> existente.getId().equals(actual.getId())
                        ? Mono.<Void>empty()
                        : Mono.error(new RecintoDuplicadoException(
                                "Ya existe un recinto con el nombre '" + nuevoNombre + "' en la ciudad '" + nuevaCiudad + "'")))
                .then();
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

