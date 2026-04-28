package com.ticketseller.application.checkout;

import com.ticketseller.domain.model.venta.EstadoVenta;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LiberarReservaUseCaseTest {

    @Test
    void deberiaLiberarReservasExpiradas() {
        VentaRepositoryPort ventaRepositoryPort = mock(VentaRepositoryPort.class);
        TicketRepositoryPort ticketRepositoryPort = mock(TicketRepositoryPort.class);

        LiberarReservaUseCase useCase = new LiberarReservaUseCase(ventaRepositoryPort, ticketRepositoryPort);

        Venta venta = Venta.builder()
                .id(UUID.randomUUID())
                .estado(EstadoVenta.RESERVADA)
                .fechaExpiracion(LocalDateTime.now().minusMinutes(1))
                .build();

        when(ventaRepositoryPort.buscarVentasExpiradas(any())).thenReturn(Flux.just(venta));
        when(ticketRepositoryPort.actualizarEstadoPorVenta(any(), any())).thenReturn(Mono.empty());
        when(ventaRepositoryPort.actualizarEstado(any(), any())).thenReturn(Mono.just(venta.toBuilder().estado(EstadoVenta.EXPIRADA).build()));

        StepVerifier.create(useCase.ejecutar())
                .verifyComplete();
    }
}

