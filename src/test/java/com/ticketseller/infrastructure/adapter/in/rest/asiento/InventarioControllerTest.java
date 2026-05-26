package com.ticketseller.infrastructure.adapter.in.rest.asiento;

import com.ticketseller.application.inventario.AsientoEnEvento;
import com.ticketseller.application.inventario.ConfirmarOcupacionUseCase;
import com.ticketseller.application.inventario.LiberarHoldUseCase;
import com.ticketseller.application.inventario.ObtenerInventarioEventoUseCase;
import com.ticketseller.application.inventario.VerificarDisponibilidadUseCase;
import com.ticketseller.domain.exception.asiento.HoldExpiradoException;
import com.ticketseller.domain.model.asiento.AsientoHold;
import com.ticketseller.domain.model.asiento.EstadoHold;
import com.ticketseller.infrastructure.adapter.in.rest.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = InventarioController.class)
@Import(GlobalExceptionHandler.class)
class InventarioControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private VerificarDisponibilidadUseCase verificarDisponibilidadUseCase;

    @MockBean
    private ConfirmarOcupacionUseCase confirmarOcupacionUseCase;

    @MockBean
    private LiberarHoldUseCase liberarHoldUseCase;

    @MockBean
    private ObtenerInventarioEventoUseCase obtenerInventarioEventoUseCase;

    private final UUID eventoId = UUID.randomUUID();

    // T007: GET disponibilidad con asiento DISPONIBLE → 200, estado:DISPONIBLE
    @Test
    void disponibilidadAsientoDisponibleRetorna200ConEstadoDisponible() {
        UUID id = UUID.randomUUID();
        AsientoEnEvento resultado = new AsientoEnEvento(id, "A", 1, "A1", UUID.randomUUID(), AsientoEnEvento.DISPONIBLE);
        when(verificarDisponibilidadUseCase.ejecutar(id, eventoId)).thenReturn(Mono.just(resultado));

        webTestClient.get()
                .uri("/api/v1/eventos/{eventoId}/inventario/{id}/disponibilidad", eventoId, id)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("DISPONIBLE");
    }

    // T008: GET disponibilidad con asiento RESERVADO → 200, estado:RESERVADO
    @Test
    void disponibilidadAsientoReservadoRetorna200ConEstadoReservado() {
        UUID id = UUID.randomUUID();
        AsientoEnEvento resultado = new AsientoEnEvento(id, "A", 1, "A1", UUID.randomUUID(), AsientoEnEvento.RESERVADO);
        when(verificarDisponibilidadUseCase.ejecutar(id, eventoId)).thenReturn(Mono.just(resultado));

        webTestClient.get()
                .uri("/api/v1/eventos/{eventoId}/inventario/{id}/disponibilidad", eventoId, id)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("RESERVADO");
    }

    // T011: GET disponibilidad con asiento inexistente → 404
    @Test
    void disponibilidadAsientoInexistenteRetorna404() {
        UUID id = UUID.randomUUID();
        when(verificarDisponibilidadUseCase.ejecutar(id, eventoId)).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/api/v1/eventos/{eventoId}/inventario/{id}/disponibilidad", eventoId, id)
                .exchange()
                .expectStatus().isNotFound();
    }

    // T024: PATCH /ocupar con hold activo → 200 con estado VENDIDO
    @Test
    void ocuparAsientoConHoldActivoRetorna200ConVendido() {
        UUID id = UUID.randomUUID();
        AsientoHold hold = AsientoHold.builder()
                .asientoId(id).eventoId(eventoId).estado(EstadoHold.VENDIDO).build();
        when(confirmarOcupacionUseCase.ejecutar(id, eventoId)).thenReturn(Mono.just(hold));

        webTestClient.patch()
                .uri("/api/v1/eventos/{eventoId}/inventario/{id}/ocupar", eventoId, id)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("VENDIDO");
    }

    // T025: PATCH /liberar tras pago fallido → 200 con estado DISPONIBLE
    @Test
    void liberarAsientoTrasPagoFallidoRetorna200ConDisponible() {
        UUID id = UUID.randomUUID();
        AsientoHold hold = AsientoHold.builder()
                .asientoId(id).eventoId(eventoId).estado(EstadoHold.EXPIRADO).build();
        when(liberarHoldUseCase.ejecutar(id, eventoId)).thenReturn(Mono.just(hold));

        webTestClient.patch()
                .uri("/api/v1/eventos/{eventoId}/inventario/{id}/liberar", eventoId, id)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("DISPONIBLE");
    }

    // T026: PATCH /ocupar con hold expirado → 409
    @Test
    void ocuparConHoldExpiradoRetorna409() {
        UUID id = UUID.randomUUID();
        when(confirmarOcupacionUseCase.ejecutar(id, eventoId))
                .thenReturn(Mono.error(new HoldExpiradoException("El hold del asiento ha expirado")));

        webTestClient.patch()
                .uri("/api/v1/eventos/{eventoId}/inventario/{id}/ocupar", eventoId, id)
                .exchange()
                .expectStatus().isEqualTo(409);
    }
}
