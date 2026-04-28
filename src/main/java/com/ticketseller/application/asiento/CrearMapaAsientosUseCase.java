package com.ticketseller.application.asiento;

import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
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

    public Flux<Asiento> ejecutar(UUID recintoId, String filas, int columnasPorFila) {
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

    private List<Asiento> generarAsientos(UUID recintoId, String filas, int columnasPorFila) {
        String[] filaNombres = filas.split(",");
        List<Asiento> asientos = new ArrayList<>(filaNombres.length * columnasPorFila);
        int numero = 1;
        for (String filaNombre : filaNombres) {
            String filaLimpia = filaNombre.trim();
            for (int columna = 1; columna <= columnasPorFila; columna++) {
                asientos.add(Asiento.builder()
                        .id(UUID.randomUUID())
                        .fila(filaLimpia)
                        .columna(columna)
                        .numero(String.valueOf(numero++))
                        .zonaId(null)
                        .estado(EstadoAsiento.DISPONIBLE)
                        .build());
            }
        }
        return asientos;
    }
}
