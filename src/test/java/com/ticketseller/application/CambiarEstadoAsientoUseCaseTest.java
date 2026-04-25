package com.ticketseller.application;

import com.ticketseller.domain.exception.TransicionEstadoInvalidaException;
import com.ticketseller.domain.model.Asiento;
import com.ticketseller.domain.model.EstadoAsiento;
import com.ticketseller.domain.model.HistorialCambioEstado;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.HistorialCambioEstadoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CambiarEstadoAsientoUseCaseTest {

    private AsientoRepositoryPort asientoRepositoryPort;
    private HistorialCambioEstadoRepositoryPort historialRepositoryPort;
    private CambiarEstadoAsientoUseCase useCase;

    @BeforeEach
    void setUp() {
        asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        historialRepositoryPort = mock(HistorialCambioEstadoRepositoryPort.class);
        useCase = new CambiarEstadoAsientoUseCase(asientoRepositoryPort, historialRepositoryPort);
    }

    @Test
    void transicionValidaPersisteCambioYGuardaHistorial() {
        UUID eventoId = UUID.randomUUID();
        UUID asientoId = UUID.randomUUID();
        String usuarioId = "user1";
        
        Asiento asiento = Asiento.builder()
                .id(asientoId)
                .estado(EstadoAsiento.DISPONIBLE)
                .build();

        Asiento asientoActualizado = asiento.toBuilder().estado(EstadoAsiento.MANTENIMIENTO).build();
        
        when(asientoRepositoryPort.buscarPorId(asientoId)).thenReturn(Mono.just(asiento));
        when(asientoRepositoryPort.guardar(any(Asiento.class))).thenReturn(Mono.just(asientoActualizado));
        when(historialRepositoryPort.guardar(any(HistorialCambioEstado.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(eventoId, asientoId, EstadoAsiento.MANTENIMIENTO, "Motivo", usuarioId))
                .expectNextMatches(a -> a.getEstado() == EstadoAsiento.MANTENIMIENTO)
                .verifyComplete();

        ArgumentCaptor<HistorialCambioEstado> captor = ArgumentCaptor.forClass(HistorialCambioEstado.class);
        verify(historialRepositoryPort).guardar(captor.capture());
        
        HistorialCambioEstado historial = captor.getValue();
        assertEquals(EstadoAsiento.DISPONIBLE, historial.getEstadoAnterior());
        assertEquals(EstadoAsiento.MANTENIMIENTO, historial.getEstadoNuevo());
        assertEquals("Motivo", historial.getMotivo());
        assertEquals(usuarioId, historial.getUsuarioId());
    }

    @Test
    void transicionInvalidaLanzaExcepcion() {
        UUID eventoId = UUID.randomUUID();
        UUID asientoId = UUID.randomUUID();
        
        Asiento asiento = Asiento.builder()
                .id(asientoId)
                .estado(EstadoAsiento.VENDIDO)
                .build();

        when(asientoRepositoryPort.buscarPorId(asientoId)).thenReturn(Mono.just(asiento));

        StepVerifier.create(useCase.ejecutar(eventoId, asientoId, EstadoAsiento.DISPONIBLE, "Motivo", "user1"))
                .expectError(TransicionEstadoInvalidaException.class)
                .verify();
                
        verify(asientoRepositoryPort, never()).guardar(any());
        verify(historialRepositoryPort, never()).guardar(any());
    }
}
