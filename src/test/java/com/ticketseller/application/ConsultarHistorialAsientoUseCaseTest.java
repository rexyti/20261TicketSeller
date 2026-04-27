package com.ticketseller.application;

import com.ticketseller.domain.model.EstadoAsiento;
import com.ticketseller.domain.model.HistorialCambioEstado;
import com.ticketseller.domain.repository.HistorialCambioEstadoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarHistorialAsientoUseCaseTest {

    private HistorialCambioEstadoRepositoryPort historialRepositoryPort;
    private ConsultarHistorialAsientoUseCase useCase;

    @BeforeEach
    void setUp() {
        historialRepositoryPort = mock(HistorialCambioEstadoRepositoryPort.class);
        useCase = new ConsultarHistorialAsientoUseCase(historialRepositoryPort);
    }

    @Test
    void retornaHistorialDeAsiento() {
        UUID eventoId = UUID.randomUUID();
        UUID asientoId = UUID.randomUUID();

        HistorialCambioEstado h1 = HistorialCambioEstado.builder()
                .id(UUID.randomUUID())
                .asientoId(asientoId)
                .eventoId(eventoId)
                .usuarioId("admin")
                .estadoAnterior(EstadoAsiento.DISPONIBLE)
                .estadoNuevo(EstadoAsiento.MANTENIMIENTO)
                .fechaHora(Instant.now().minusSeconds(60))
                .motivo("Reparación")
                .build();

        HistorialCambioEstado h2 = HistorialCambioEstado.builder()
                .id(UUID.randomUUID())
                .asientoId(asientoId)
                .eventoId(eventoId)
                .usuarioId("admin")
                .estadoAnterior(EstadoAsiento.MANTENIMIENTO)
                .estadoNuevo(EstadoAsiento.DISPONIBLE)
                .fechaHora(Instant.now())
                .motivo("Reparación completada")
                .build();

        when(historialRepositoryPort.findByAsientoId(asientoId)).thenReturn(Flux.just(h1, h2));

        StepVerifier.create(useCase.ejecutar(eventoId, asientoId))
                .expectNextMatches(h -> h.getEstadoAnterior() == EstadoAsiento.DISPONIBLE
                        && h.getEstadoNuevo() == EstadoAsiento.MANTENIMIENTO
                        && "Reparación".equals(h.getMotivo()))
                .expectNextMatches(h -> h.getEstadoAnterior() == EstadoAsiento.MANTENIMIENTO
                        && h.getEstadoNuevo() == EstadoAsiento.DISPONIBLE)
                .verifyComplete();
    }

    @Test
    void retornaFluxVacioCuandoNoHayHistorial() {
        UUID eventoId = UUID.randomUUID();
        UUID asientoId = UUID.randomUUID();

        when(historialRepositoryPort.findByAsientoId(asientoId)).thenReturn(Flux.empty());

        StepVerifier.create(useCase.ejecutar(eventoId, asientoId))
                .verifyComplete();
    }
}
