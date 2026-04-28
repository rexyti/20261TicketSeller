package com.ticketseller.application.asiento;

import com.ticketseller.domain.model.asiento.HistorialCambioEstado;
import com.ticketseller.domain.repository.HistorialCambioEstadoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RequiredArgsConstructor
public class ConsultarHistorialAsientoUseCase {
    private final HistorialCambioEstadoRepositoryPort historialRepositoryPort;

    public Flux<HistorialCambioEstado> ejecutar(UUID eventoId, UUID asientoId) {
        // En una implementación real más compleja podríamos validar que el asiento pertenezca al evento
        return historialRepositoryPort.findByAsientoId(asientoId);
    }
}
