package com.ticketseller.application.mantenimiento;

import com.ticketseller.domain.model.Asiento;
import com.ticketseller.domain.model.EstadoAsiento;
import com.ticketseller.domain.model.EstadoTicket;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.HistorialCambioEstadoRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.infrastructure.adapter.in.rest.dto.CambiarEstadoMasivoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CambiarEstadoMasivoUseCaseTest {

    private AsientoRepositoryPort asientoRepositoryPort;
    private HistorialCambioEstadoRepositoryPort historialRepositoryPort;
    private TicketRepositoryPort ticketRepositoryPort;
    private CambiarEstadoMasivoUseCase useCase;

    @BeforeEach
    void setUp() {
        asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        historialRepositoryPort = mock(HistorialCambioEstadoRepositoryPort.class);
        ticketRepositoryPort = mock(TicketRepositoryPort.class);
        useCase = new CambiarEstadoMasivoUseCase(asientoRepositoryPort, historialRepositoryPort, ticketRepositoryPort);
    }

    @Test
    void cambioMasivoExitoso() {
        UUID eventoId = UUID.randomUUID();
        UUID asientoId1 = UUID.randomUUID();
        UUID asientoId2 = UUID.randomUUID();
        
        Asiento asiento1 = Asiento.builder().id(asientoId1).estado(EstadoAsiento.DISPONIBLE).build();
        Asiento asiento2 = Asiento.builder().id(asientoId2).estado(EstadoAsiento.DISPONIBLE).build();

        when(asientoRepositoryPort.buscarPorId(asientoId1)).thenReturn(Mono.just(asiento1));
        when(asientoRepositoryPort.buscarPorId(asientoId2)).thenReturn(Mono.just(asiento2));
        when(ticketRepositoryPort.buscarPorAsiento(any())).thenReturn(Mono.empty());
        when(asientoRepositoryPort.guardar(any())).thenReturn(Mono.just(asiento1));
        when(historialRepositoryPort.guardar(any())).thenReturn(Mono.just(mock(com.ticketseller.domain.model.HistorialCambioEstado.class)));

        StepVerifier.create(useCase.ejecutar(eventoId, List.of(asientoId1, asientoId2), EstadoAsiento.MANTENIMIENTO, "Lote", "user1"))
                .assertNext(response -> {
                    assertEquals(2, response.modificados());
                    assertEquals(0, response.omitidos());
                    assertEquals(0, response.mensajes().size());
                })
                .verifyComplete();
    }

    @Test
    void cambioMasivoConOmitidosPorCompraActiva() {
        UUID eventoId = UUID.randomUUID();
        UUID asientoId1 = UUID.randomUUID();
        
        Asiento asiento1 = Asiento.builder().id(asientoId1).estado(EstadoAsiento.DISPONIBLE).build();

        when(asientoRepositoryPort.buscarPorId(asientoId1)).thenReturn(Mono.just(asiento1));
        when(ticketRepositoryPort.buscarPorAsiento(asientoId1)).thenReturn(Mono.just(com.ticketseller.domain.model.Ticket.builder().estado(EstadoTicket.VENDIDO).build()));

        StepVerifier.create(useCase.ejecutar(eventoId, List.of(asientoId1), EstadoAsiento.MANTENIMIENTO, "Lote", "user1"))
                .assertNext(response -> {
                    assertEquals(0, response.modificados());
                    assertEquals(1, response.omitidos());
                    assertEquals(1, response.mensajes().size());
                })
                .verifyComplete();
    }
}
