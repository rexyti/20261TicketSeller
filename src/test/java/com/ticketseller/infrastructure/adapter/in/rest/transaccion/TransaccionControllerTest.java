package com.ticketseller.infrastructure.adapter.in.rest.transaccion;

import com.ticketseller.application.transaccion.CambiarEstadoVentaUseCase;
import com.ticketseller.application.transaccion.ConsultarHistorialVentaUseCase;
import com.ticketseller.application.transaccion.ListarTransaccionesUseCase;
import com.ticketseller.domain.exception.transaccion.TransicionVentaInvalidaException;
import com.ticketseller.domain.exception.transaccion.VentaNoEncontradaException;
import com.ticketseller.domain.model.transaccion.HistorialEstadoVenta;
import com.ticketseller.domain.model.venta.EstadoVenta;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.infrastructure.adapter.in.rest.GlobalExceptionHandler;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.TransaccionRestMapperImpl;
import com.ticketseller.infrastructure.adapter.in.rest.transaccion.dto.CambiarEstadoVentaRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = TransaccionController.class)
@Import({GlobalExceptionHandler.class, TransaccionRestMapperImpl.class})
class TransaccionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CambiarEstadoVentaUseCase cambiarEstadoVentaUseCase;

    @MockBean
    private ConsultarHistorialVentaUseCase consultarHistorialVentaUseCase;

    @MockBean
    private ListarTransaccionesUseCase listarTransaccionesUseCase;

    @Test
    void cambiarEstadoTransicionValidaRetorna200() {
        UUID ventaId = UUID.randomUUID();
        Venta ventaActualizada = venta(ventaId, EstadoVenta.COMPLETADA);
        CambiarEstadoVentaRequest request = new CambiarEstadoVentaRequest(EstadoVenta.COMPLETADA, "Pago confirmado", null);

        when(cambiarEstadoVentaUseCase.ejecutar(any(), any(), any(), any())).thenReturn(Mono.just(ventaActualizada));

        webTestClient.patch()
                .uri("/api/v1/admin/ventas/{id}/estado", ventaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("COMPLETADA");
    }

    @Test
    void cambiarEstadoTransicionInvalidaRetorna422() {
        UUID ventaId = UUID.randomUUID();
        CambiarEstadoVentaRequest request = new CambiarEstadoVentaRequest(EstadoVenta.PENDIENTE, "Retrodecer", null);

        when(cambiarEstadoVentaUseCase.ejecutar(any(), any(), any(), any()))
                .thenReturn(Mono.error(new TransicionVentaInvalidaException(EstadoVenta.COMPLETADA, EstadoVenta.PENDIENTE)));

        webTestClient.patch()
                .uri("/api/v1/admin/ventas/{id}/estado", ventaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(422);
    }

    @Test
    void consultarHistorialRetornaListaOrdenada() {
        UUID ventaId = UUID.randomUUID();
        HistorialEstadoVenta historial = HistorialEstadoVenta.builder()
                .id(UUID.randomUUID()).ventaId(ventaId)
                .estadoAnterior(EstadoVenta.PENDIENTE).estadoNuevo(EstadoVenta.RESERVADA)
                .justificacion("reserva").fechaCambio(LocalDateTime.now())
                .build();

        when(consultarHistorialVentaUseCase.ejecutar(ventaId)).thenReturn(Flux.just(historial));

        webTestClient.get()
                .uri("/api/v1/admin/ventas/{id}/historial", ventaId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].estadoAnterior").isEqualTo("PENDIENTE")
                .jsonPath("$[0].estadoNuevo").isEqualTo("RESERVADA");
    }

    @Test
    void consultarHistorialVentaNoEncontradaRetorna404() {
        UUID ventaId = UUID.randomUUID();
        when(consultarHistorialVentaUseCase.ejecutar(ventaId))
                .thenReturn(Flux.error(new VentaNoEncontradaException(ventaId)));

        webTestClient.get()
                .uri("/api/v1/admin/ventas/{id}/historial", ventaId)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void listarTransaccionesFiltradaRetorna200() {
        when(listarTransaccionesUseCase.ejecutar(any())).thenReturn(Flux.just(venta(UUID.randomUUID(), EstadoVenta.FALLIDA)));

        webTestClient.get()
                .uri("/api/v1/admin/ventas?estado=FALLIDA")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].estado").isEqualTo("FALLIDA");
    }

    @Test
    void listarTransaccionesSinResultadosRetornaListaVacia() {
        when(listarTransaccionesUseCase.ejecutar(any())).thenReturn(Flux.empty());

        webTestClient.get()
                .uri("/api/v1/admin/ventas")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.length()").isEqualTo(0);
    }

    private Venta venta(UUID id, EstadoVenta estado) {
        return Venta.builder()
                .id(id).compradorId(UUID.randomUUID()).eventoId(UUID.randomUUID())
                .estado(estado).total(BigDecimal.valueOf(100))
                .fechaCreacion(LocalDateTime.now()).fechaExpiracion(LocalDateTime.now().plusHours(1))
                .build();
    }
}
