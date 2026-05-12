package com.ticketseller.application.asiento;

import com.ticketseller.domain.exception.asiento.RecintoConZonasYaActivasException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.MapaAsientosRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class CrearMapaAsientosUseCase {

    private final AsientoRepositoryPort asientoRepositoryPort;
    private final MapaAsientosRepositoryPort mapaAsientosRepositoryPort;

    public Flux<Asiento> ejecutar(UUID recintoId, int filas, int columnasPorFila) {
        return mapaAsientosRepositoryPort.tieneZonasActivas(recintoId)
                .filter(tieneZonas -> !tieneZonas)
                .switchIfEmpty(Mono.error(new RecintoConZonasYaActivasException(
                        "El recinto tiene zonas activas. No se puede crear un mapa de asientos en un recinto con zonas.")))
                .flatMapMany(tieneZonas -> {
                    List<Asiento> asientos = generarAsientos(recintoId, filas, columnasPorFila);
                    return asientoRepositoryPort.guardarTodos(asientos);
                });
    }

    private List<Asiento> generarAsientos(UUID recintoId, int numFilas, int columnasPorFila) {
        List<Asiento> asientos = new ArrayList<>(numFilas * columnasPorFila);
        for (int fila = 1; fila <= numFilas; fila++) {
            String nombreFila = generarNombreFila(fila);
            for (int columna = 1; columna <= columnasPorFila; columna++) {
                asientos.add(crearAsiento(nombreFila, columna, recintoId));
            }
        }
        return asientos;
    }

    private String generarNombreFila(int indice) {
        StringBuilder nombre = new StringBuilder();
        while (indice > 0) {
            indice--;
            nombre.insert(0, (char) ('A' + (indice % 26)));
            indice /= 26;
        }
        return nombre.toString();
    }

    private Asiento crearAsiento(String fila, int columna, UUID recintoId) {
        return Asiento.builder()
                .fila(fila)
                .columna(columna)
                .zonaId(null)
                .recintoId(recintoId)
                .estado(EstadoAsiento.DISPONIBLE)
                .build()
                .normalizarDatosRegistro();
    }
}
