package com.ticketseller.application.transaccion;

import com.ticketseller.domain.model.venta.EstadoVenta;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListarTransaccionesUseCaseTest {

    private final VentaRepositoryPort ventaRepositoryPort = mock(VentaRepositoryPort.class);
    private final ListarTransaccionesUseCase useCase = new ListarTransaccionesUseCase(ventaRepositoryPort);

    @Test
    void deberiaRetornarVentasFiltradas() {
        UUID ventaId = UUID.randomUUID();
        Venta venta = Venta.builder()
                .id(ventaId).estado(EstadoVenta.FALLIDA)
                .total(BigDecimal.TEN).fechaCreacion(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusHours(1))
                .build();

        when(ventaRepositoryPort.buscarConFiltros(eq(EstadoVenta.FALLIDA), any(), any(), any()))
                .thenReturn(Flux.just(venta));

        FiltroTransacciones filtro = new FiltroTransacciones(EstadoVenta.FALLIDA, null, null, null);
        StepVerifier.create(useCase.ejecutar(filtro))
                .assertNext(v -> assertEquals(EstadoVenta.FALLIDA, v.getEstado()))
                .verifyComplete();
    }

    @Test
    void deberiaRetornarFluxVacioSinResultados() {
        when(ventaRepositoryPort.buscarConFiltros(any(), any(), any(), any())).thenReturn(Flux.empty());

        FiltroTransacciones filtro = new FiltroTransacciones(EstadoVenta.EXPIRADA, null, null, null);
        StepVerifier.create(useCase.ejecutar(filtro))
                .verifyComplete();
    }
}
