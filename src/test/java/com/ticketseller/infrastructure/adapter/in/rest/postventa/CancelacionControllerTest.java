package com.ticketseller.infrastructure.adapter.in.rest.postventa;

import com.ticketseller.application.postventa.CancelacionResultado;
import com.ticketseller.application.postventa.CancelarTicketUseCase;
import com.ticketseller.application.postventa.ProcesarReembolsoMasivoUseCase;
import com.ticketseller.domain.exception.postventa.CancelacionFueraDePlazoException;
import com.ticketseller.domain.exception.postventa.TicketYaUsadoException;
import com.ticketseller.infrastructure.adapter.in.rest.GlobalExceptionHandler;
import com.ticketseller.infrastructure.adapter.in.rest.postventa.dto.CancelacionResponse;
import com.ticketseller.infrastructure.adapter.in.rest.postventa.dto.CancelarTicketRequest;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.PostVentaRestMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = CancelacionController.class)
@Import(GlobalExceptionHandler.class)
class CancelacionControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CancelarTicketUseCase cancelarTicketUseCase;
    @MockBean
    private ProcesarReembolsoMasivoUseCase procesarReembolsoMasivoUseCase;
    @MockBean
    private PostVentaRestMapper postVentaRestMapper;

    @Test
    void cancelarTicketValidoRetorna200() {
        UUID ticketId = UUID.randomUUID();
        CancelacionResultado resultado = new CancelacionResultado(List.of(ticketId), UUID.randomUUID(),
                BigDecimal.TEN, List.of());
        CancelacionResponse response = new CancelacionResponse(resultado.ticketsCancelados(), resultado.reembolsoId(),
                resultado.montoPendiente());
        when(cancelarTicketUseCase.cancelarTicket(ticketId)).thenReturn(Mono.just(resultado));
        when(postVentaRestMapper.toCancelacionResponse(resultado)).thenReturn(response);

        webTestClient.post()
                .uri("/api/v1/tickets/{id}/cancelar", ticketId)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.ticketsCancelados[0]").isEqualTo(ticketId.toString());
    }

    @Test
    void cancelarTicketUsadoRetorna409() {
        UUID ticketId = UUID.randomUUID();
        when(cancelarTicketUseCase.cancelarTicket(ticketId))
                .thenReturn(Mono.error(new TicketYaUsadoException("usado")));

        webTestClient.post()
                .uri("/api/v1/tickets/{id}/cancelar", ticketId)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void cancelarTicketFueraDePlazoRetorna422() {
        UUID ticketId = UUID.randomUUID();
        when(cancelarTicketUseCase.cancelarTicket(ticketId))
                .thenReturn(Mono.error(new CancelacionFueraDePlazoException("fuera de plazo")));

        webTestClient.post()
                .uri("/api/v1/tickets/{id}/cancelar", ticketId)
                .exchange()
                .expectStatus().isEqualTo(422);
    }

    @Test
    void cancelarParcialCancelaSoloSeleccionados() {
        UUID t1 = UUID.randomUUID();
        CancelarTicketRequest request = new CancelarTicketRequest(List.of(t1));
        CancelacionResultado resultado = new CancelacionResultado(List.of(t1), UUID.randomUUID(),
                BigDecimal.valueOf(50), List.of());
        CancelacionResponse response = new CancelacionResponse(resultado.ticketsCancelados(), resultado.reembolsoId(),
                resultado.montoPendiente());
        when(cancelarTicketUseCase.cancelarVarios(request.ticketIds())).thenReturn(Mono.just(resultado));
        when(postVentaRestMapper.toCancelacionResponse(resultado)).thenReturn(response);

        webTestClient.post()
                .uri("/api/v1/tickets/cancelar-parcial")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.ticketsCancelados.length()").isEqualTo(1);
    }

    @Test
    void cancelarEventoDisparaProcesoMasivo() {
        UUID eventoId = UUID.randomUUID();
        when(procesarReembolsoMasivoUseCase.ejecutar(eq(eventoId))).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/api/v1/tickets/eventos/{eventoId}/cancelar", eventoId)
                .exchange()
                .expectStatus().isOk();
    }
}

