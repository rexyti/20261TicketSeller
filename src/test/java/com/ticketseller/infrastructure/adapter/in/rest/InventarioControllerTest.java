package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.ConfirmarOcupacionUseCase;
import com.ticketseller.application.LiberarAsientoUseCase;
import com.ticketseller.application.ReservarAsientoUseCase;
import com.ticketseller.application.VerificarDisponibilidadUseCase;
import com.ticketseller.domain.exception.AsientoReservadoPorOtroException;
import com.ticketseller.infrastructure.adapter.in.rest.dto.DisponibilidadResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.ReservarAsientoRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = InventarioController.class)
@Import(GlobalExceptionHandler.class)
class InventarioControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private VerificarDisponibilidadUseCase verificarDisponibilidadUseCase;

    @MockBean
    private ReservarAsientoUseCase reservarAsientoUseCase;

    @MockBean
    private ConfirmarOcupacionUseCase confirmarOcupacionUseCase;

    @MockBean
    private LiberarAsientoUseCase liberarAsientoUseCase;

    @Test
    void disponibilidadAsientoLibreRetorna200ConDisponibleTrue() {
        UUID asientoId = UUID.randomUUID();
        when(verificarDisponibilidadUseCase.ejecutar(asientoId))
                .thenReturn(Mono.just(new DisponibilidadResponse(asientoId, true, null)));

        webTestClient.get()
                .uri("/api/inventario/asientos/{id}/disponibilidad", asientoId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.asientoId").isEqualTo(asientoId.toString())
                .jsonPath("$.disponible").isEqualTo(true);
    }

    @Test
    void disponibilidadAsientoNoLibreRetorna200ConDisponibleFalse() {
        UUID asientoId = UUID.randomUUID();
        when(verificarDisponibilidadUseCase.ejecutar(asientoId))
                .thenReturn(Mono.just(new DisponibilidadResponse(asientoId, false, "ASIENTO NO DISPONIBLE")));

        webTestClient.get()
                .uri("/api/inventario/asientos/{id}/disponibilidad", asientoId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.disponible").isEqualTo(false)
                .jsonPath("$.mensaje").isEqualTo("ASIENTO NO DISPONIBLE");
    }

    @Test
    void reservarAsientoDisponibleRetorna201() {
        UUID asientoId = UUID.randomUUID();
        UUID ventaId = UUID.randomUUID();
        when(reservarAsientoUseCase.ejecutar(eq(asientoId), eq(ventaId)))
                .thenReturn(Mono.just(new DisponibilidadResponse(asientoId, false, "RESERVADO")));

        webTestClient.post()
                .uri("/api/inventario/asientos/{id}/reservar", asientoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ReservarAsientoRequest(ventaId))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.mensaje").isEqualTo("RESERVADO");
    }

    @Test
    void reservarAsientoYaReservadoRetorna409() {
        UUID asientoId = UUID.randomUUID();
        UUID ventaId = UUID.randomUUID();
        String mensaje = "ASIENTO NO DISPONIBLE - OTRO USUARIO ESTÁ COMPRANDO ESTE ASIENTO";
        when(reservarAsientoUseCase.ejecutar(eq(asientoId), eq(ventaId)))
                .thenReturn(Mono.error(new AsientoReservadoPorOtroException(mensaje)));

        webTestClient.post()
                .uri("/api/inventario/asientos/{id}/reservar", asientoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ReservarAsientoRequest(ventaId))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.mensaje").isEqualTo(mensaje);
    }

    @Test
    void ocuparAsientoRetorna200() {
        UUID asientoId = UUID.randomUUID();
        when(confirmarOcupacionUseCase.ejecutar(asientoId)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/api/inventario/asientos/{id}/ocupar", asientoId)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void liberarAsientoRetorna200() {
        UUID asientoId = UUID.randomUUID();
        when(liberarAsientoUseCase.ejecutar(any(UUID.class))).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/api/inventario/asientos/{id}/liberar", asientoId)
                .exchange()
                .expectStatus().isOk();
    }
}
