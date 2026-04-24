package com.ticketseller.application.tipoasiento;

import com.ticketseller.domain.model.Asiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.MapaAsientosRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class CrearMapaAsientosUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;
    private final MapaAsientosRepositoryPort mapaAsientosRepositoryPort;

    public Flux<Asiento> ejecutar(UUID recintoId, int filas, int columnasPorFila) {
        return mapaAsientosRepositoryPort.tieneZonasActivas(recintoId)
                .flatMapMany(tieneZonas -> {
                    if (tieneZonas) {
                        return Flux.error(new IllegalStateException(
                                "El recinto tiene zonas activas. No se puede crear un mapa de asientos en un recinto con zonas."));
                    }
                    List<Asiento> asientos = generarAsientos(recintoId, filas, columnasPorFila);
                    return asientoRepositoryPort.guardarTodos(asientos);
                });
    }

    private List<Asiento> generarAsientos(UUID recintoId, int filas, int columnasPorFila) {
        List<Asiento> asientos = new ArrayList<>(filas * columnasPorFila);
        int numero = 1;
        for (int fila = 1; fila <= filas; fila++) {
            for (int columna = 1; columna <= columnasPorFila; columna++) {
                asientos.add(Asiento.builder()
                        .id(UUID.randomUUID())
                        .fila(fila)
                        .columna(columna)
                        .numero(String.valueOf(numero++))
                        .zonaId(null)
                        .estado(com.ticketseller.domain.model.EstadoAsiento.DISPONIBLE)
                        .build());
            }
        }
        return asientos;
    }
}
