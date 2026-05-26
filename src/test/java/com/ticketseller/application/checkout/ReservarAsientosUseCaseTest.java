package com.ticketseller.application.checkout;

import com.ticketseller.application.promocion.AplicarDescuentoCarritoUseCase;
import com.ticketseller.application.promocion.DescuentoAplicado;
import com.ticketseller.domain.exception.asiento.AsientoNoDisponibleException;
import com.ticketseller.domain.model.zona.Compuerta;
import com.ticketseller.domain.model.zona.PrecioZona;
import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.domain.model.venta.EstadoVenta;
import com.ticketseller.domain.repository.AsientoHoldRepositoryPort;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.BloqueoRepositoryPort;
import com.ticketseller.domain.repository.CompuertaRepositoryPort;
import com.ticketseller.domain.repository.PrecioZonaRepositoryPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReservarAsientosUseCaseTest {

    @Test
    void deberiaReservarCuandoHayDisponibilidad() {
        TicketRepositoryPort ticketRepositoryPort = mock(TicketRepositoryPort.class);
        VentaRepositoryPort ventaRepositoryPort = mock(VentaRepositoryPort.class);
        ZonaRepositoryPort zonaRepositoryPort = mock(ZonaRepositoryPort.class);
        PrecioZonaRepositoryPort precioZonaRepositoryPort = mock(PrecioZonaRepositoryPort.class);
        AsientoRepositoryPort asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        AsientoHoldRepositoryPort asientoHoldRepositoryPort = mock(AsientoHoldRepositoryPort.class);
        AplicarDescuentoCarritoUseCase aplicarDescuentoCarritoUseCase = mock(AplicarDescuentoCarritoUseCase.class);

        BloqueoRepositoryPort bloqueoRepositoryPort = mock(BloqueoRepositoryPort.class);
        ReservarAsientosUseCase useCase = new ReservarAsientosUseCase(ticketRepositoryPort, ventaRepositoryPort,
                zonaRepositoryPort, precioZonaRepositoryPort, asientoRepositoryPort, asientoHoldRepositoryPort,
                bloqueoRepositoryPort, aplicarDescuentoCarritoUseCase);

        UUID eventoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();

        when(zonaRepositoryPort.buscarPorId(zonaId)).thenReturn(Mono.just(
                Zona.builder().id(zonaId).nombre("Zona Norte").capacidad(100).build()));
        when(precioZonaRepositoryPort.buscarPorEvento(eventoId)).thenReturn(Flux.just(
                PrecioZona.builder().id(UUID.randomUUID()).eventoId(eventoId).zonaId(zonaId).precio(BigDecimal.TEN).build()));
        when(ticketRepositoryPort.contarPorEventoYZonaYEstados(any(), any(), anySet())).thenReturn(Mono.just(1L));
        when(aplicarDescuentoCarritoUseCase.ejecutar(any(), any(), anyList()))
                .thenReturn(Mono.just(new DescuentoAplicado(
                        new BigDecimal("20"), BigDecimal.ZERO, new BigDecimal("20"))));
        when(ventaRepositoryPort.guardar(any())).thenAnswer(invocation -> {
            com.ticketseller.domain.model.venta.Venta v = invocation.getArgument(0);
            return Mono.just(v.toBuilder().id(UUID.randomUUID()).build());
        });

        ReservarAsientosCommand command = new ReservarAsientosCommand(UUID.randomUUID(), eventoId, zonaId, 2, false, null, null);

        StepVerifier.create(useCase.ejecutar(command))
                .assertNext(detalle -> {
                    assert detalle.venta().getId() != null;
                    assert detalle.venta().getEstado() == EstadoVenta.RESERVADA;
                    assert detalle.venta().getZonaId().equals(zonaId);
                    assert detalle.venta().getCantidad() == 2;
                    assert detalle.tickets().isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void deberiaFallarSiNoHayDisponibilidad() {
        TicketRepositoryPort ticketRepositoryPort = mock(TicketRepositoryPort.class);
        VentaRepositoryPort ventaRepositoryPort = mock(VentaRepositoryPort.class);
        ZonaRepositoryPort zonaRepositoryPort = mock(ZonaRepositoryPort.class);
        PrecioZonaRepositoryPort precioZonaRepositoryPort = mock(PrecioZonaRepositoryPort.class);
        AsientoRepositoryPort asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        AsientoHoldRepositoryPort asientoHoldRepositoryPort = mock(AsientoHoldRepositoryPort.class);
        AplicarDescuentoCarritoUseCase aplicarDescuentoCarritoUseCase = mock(AplicarDescuentoCarritoUseCase.class);

        BloqueoRepositoryPort bloqueoRepositoryPort = mock(BloqueoRepositoryPort.class);
        ReservarAsientosUseCase useCase = new ReservarAsientosUseCase(ticketRepositoryPort, ventaRepositoryPort,
                zonaRepositoryPort, precioZonaRepositoryPort, asientoRepositoryPort, asientoHoldRepositoryPort,
                bloqueoRepositoryPort, aplicarDescuentoCarritoUseCase);

        UUID eventoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();

        when(zonaRepositoryPort.buscarPorId(zonaId)).thenReturn(Mono.just(
                Zona.builder().id(zonaId).nombre("Zona Sur").capacidad(2).build()));
        when(precioZonaRepositoryPort.buscarPorEvento(eventoId)).thenReturn(Flux.just(
                PrecioZona.builder().id(UUID.randomUUID()).eventoId(eventoId).zonaId(zonaId).precio(BigDecimal.TEN).build()));
        when(ticketRepositoryPort.contarPorEventoYZonaYEstados(any(), any(), anySet())).thenReturn(Mono.just(2L));

        ReservarAsientosCommand command = new ReservarAsientosCommand(UUID.randomUUID(), eventoId, zonaId, 1, false, null, null);

        StepVerifier.create(useCase.ejecutar(command))
                .expectError(AsientoNoDisponibleException.class)
                .verify();
    }
}
