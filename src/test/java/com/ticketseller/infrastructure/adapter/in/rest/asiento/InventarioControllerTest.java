package com.ticketseller.infrastructure.adapter.in.rest.asiento;

import com.ticketseller.application.inventario.ConfirmarOcupacionUseCase;
import com.ticketseller.application.inventario.ReservarAsientoUseCase;
import com.ticketseller.application.inventario.VerificarDisponibilidadUseCase;
import com.ticketseller.domain.exception.asiento.AsientoNoDisponibleException;
import com.ticketseller.domain.exception.asiento.AsientoReservadoPorOtroException;
import com.ticketseller.domain.exception.asiento.HoldExpiradoException;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.infrastructure.adapter.in.rest.GlobalExceptionHandler;
import com.ticketseller.infrastructure.adapter.in.rest.asiento.dto.ReservarAsientoRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
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
    private ReservarAsientoUseCase reservarAsientoUseCase;

    @MockBean
    private ConfirmarOcupacionUseCase confirmarOcupacionUseCase;

    // T007: GET disponibilidad con asiento DISPONIBLE → 200, disponible:true
    @Test
    void disponibilidadAsientoDisponibleRetorna200ConDisponibleTrue() {
        UUID id = UUID.randomUUID();
        Asiento asiento = Asiento.builder().id(id).estado(EstadoAsiento.DISPONIBLE).build();
        when(verificarDisponibilidadUseCase.ejecutar(id)).thenReturn(Mono.just(asiento));

        webTestClient.get()
                .uri("/api/inventario/asientos/{id}/disponibilidad", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.disponible").isEqualTo(true)
                .jsonPath("$.estado").isEqualTo("DISPONIBLE");
    }

    // T008: GET disponibilidad con asiento OCUPADO → 200, disponible:false
    @Test
    void disponibilidadAsientoOcupadoRetorna200ConDisponibleFalse() {
        UUID id = UUID.randomUUID();
        Asiento asiento = Asiento.builder().id(id).estado(EstadoAsiento.OCUPADO).build();
        when(verificarDisponibilidadUseCase.ejecutar(id)).thenReturn(Mono.just(asiento));

        webTestClient.get()
                .uri("/api/inventario/asientos/{id}/disponibilidad", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.disponible").isEqualTo(false)
                .jsonPath("$.estado").isEqualTo("OCUPADO");
    }

    // T009: GET disponibilidad con asiento RESERVADO → 200, disponible:false
    @Test
    void disponibilidadAsientoReservadoRetorna200ConDisponibleFalse() {
        UUID id = UUID.randomUUID();
        Asiento asiento = Asiento.builder().id(id).estado(EstadoAsiento.RESERVADO).build();
        when(verificarDisponibilidadUseCase.ejecutar(id)).thenReturn(Mono.just(asiento));

        webTestClient.get()
                .uri("/api/inventario/asientos/{id}/disponibilidad", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.disponible").isEqualTo(false)
                .jsonPath("$.estado").isEqualTo("RESERVADO");
    }

    // T011: GET disponibilidad con asiento inexistente → 404
    @Test
    void disponibilidadAsientoInexistenteRetorna404() {
        UUID id = UUID.randomUUID();
        when(verificarDisponibilidadUseCase.ejecutar(id)).thenReturn(Mono.empty());

        webTestClient.get()
                .uri("/api/inventario/asientos/{id}/disponibilidad", id)
                .exchange()
                .expectStatus().isNotFound();
    }

    // T015: POST reservar asiento disponible → 201 con estado RESERVADO y expiraEn
    @Test
    void reservarAsientoDisponibleRetorna201ConReservadoYExpiraEn() {
        UUID id = UUID.randomUUID();
        LocalDateTime expiraEn = LocalDateTime.now().plusMinutes(15);
        Asiento reservado = Asiento.builder().id(id).estado(EstadoAsiento.RESERVADO)
                .expiraEn(expiraEn).build();
        when(reservarAsientoUseCase.ejecutar(id)).thenReturn(Mono.just(reservado));

        webTestClient.post()
                .uri("/api/inventario/asientos/{id}/reservar", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ReservarAsientoRequest(UUID.randomUUID()))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("RESERVADO")
                .jsonPath("$.expiraEn").isNotEmpty();
    }

    // T016: POST reservar segundo intento → 409 con mensaje descriptivo
    @Test
    void reservarAsientoYaReservadoRetorna409() {
        UUID id = UUID.randomUUID();
        when(reservarAsientoUseCase.ejecutar(id)).thenReturn(
                Mono.error(new AsientoReservadoPorOtroException(
                        "ASIENTO NO DISPONIBLE - OTRO USUARIO ESTÁ COMPRANDO ESTE ASIENTO")));

        webTestClient.post()
                .uri("/api/inventario/asientos/{id}/reservar", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ReservarAsientoRequest(UUID.randomUUID()))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.mensaje").isEqualTo(
                        "ASIENTO NO DISPONIBLE - OTRO USUARIO ESTÁ COMPRANDO ESTE ASIENTO");
    }

    // T017: Scheduler libera holds vencidos (simulado: GET após expiração retorna DISPONIBLE)
    @Test
    void disponibilidadRetornaDisponibleTrasTiempoExpiracion() {
        UUID id = UUID.randomUUID();
        Asiento disponible = Asiento.builder().id(id).estado(EstadoAsiento.DISPONIBLE).build();
        when(verificarDisponibilidadUseCase.ejecutar(id)).thenReturn(Mono.just(disponible));

        webTestClient.get()
                .uri("/api/inventario/asientos/{id}/disponibilidad", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.disponible").isEqualTo(true);
    }

    // T024: POST /ocupar con pago confirmado → 200 con estado OCUPADO
    @Test
    void ocuparAsientoConPagoConfirmadoRetorna200ConOcupado() {
        UUID id = UUID.randomUUID();
        Asiento ocupado = Asiento.builder().id(id).estado(EstadoAsiento.OCUPADO).build();
        when(confirmarOcupacionUseCase.confirmar(id)).thenReturn(Mono.just(ocupado));

        webTestClient.post()
                .uri("/api/inventario/asientos/{id}/ocupar", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("OCUPADO");
    }

    // T025: POST /liberar tras pago fallido → 200 con estado DISPONIBLE
    @Test
    void liberarAsientoTrasPagoFallidoRetorna200ConDisponible() {
        UUID id = UUID.randomUUID();
        Asiento disponible = Asiento.builder().id(id).estado(EstadoAsiento.DISPONIBLE).build();
        when(confirmarOcupacionUseCase.liberar(id)).thenReturn(Mono.just(disponible));

        webTestClient.post()
                .uri("/api/inventario/asientos/{id}/liberar", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("DISPONIBLE")
                .jsonPath("$.disponible").isEqualTo(true);
    }

    // T031: Concurrencia — primer request 201, segundo 409
    @Test
    void primerReservaExitosaSegundaRetorna409() {
        UUID id = UUID.randomUUID();
        LocalDateTime expiraEn = LocalDateTime.now().plusMinutes(15);
        Asiento reservado = Asiento.builder().id(id).estado(EstadoAsiento.RESERVADO)
                .expiraEn(expiraEn).build();
        when(reservarAsientoUseCase.ejecutar(id))
                .thenReturn(Mono.just(reservado))
                .thenReturn(Mono.error(new AsientoReservadoPorOtroException(
                        "ASIENTO NO DISPONIBLE - OTRO USUARIO ESTÁ COMPRANDO ESTE ASIENTO")));

        webTestClient.post()
                .uri("/api/inventario/asientos/{id}/reservar", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ReservarAsientoRequest(UUID.randomUUID()))
                .exchange()
                .expectStatus().isCreated();

        webTestClient.post()
                .uri("/api/inventario/asientos/{id}/reservar", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ReservarAsientoRequest(UUID.randomUUID()))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    // T026: Confirmar con hold expirado → 409
    @Test
    void ocuparConHoldExpiradoRetorna409() {
        UUID id = UUID.randomUUID();
        when(confirmarOcupacionUseCase.confirmar(id))
                .thenReturn(Mono.error(new HoldExpiradoException("El hold del asiento ha expirado")));

        webTestClient.post()
                .uri("/api/inventario/asientos/{id}/ocupar", id)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    // T032: Use case con AsientoNoDisponibleException → 409
    @Test
    void reservarAsientoNoEncontradoRetorna409() {
        UUID id = UUID.randomUUID();
        when(reservarAsientoUseCase.ejecutar(id))
                .thenReturn(Mono.error(new AsientoNoDisponibleException("Asiento no encontrado: " + id)));

        webTestClient.post()
                .uri("/api/inventario/asientos/{id}/reservar", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new ReservarAsientoRequest(UUID.randomUUID()))
                .exchange()
                .expectStatus().isEqualTo(409);
    }
}
