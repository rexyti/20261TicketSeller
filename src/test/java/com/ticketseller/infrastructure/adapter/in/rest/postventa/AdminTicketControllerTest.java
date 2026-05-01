package com.ticketseller.infrastructure.adapter.in.rest.postventa;

import com.ticketseller.application.postventa.CambiarEstadoTicketUseCase;
import com.ticketseller.domain.exception.postventa.TransicionEstadoInvalidaException;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.infrastructure.adapter.in.rest.GlobalExceptionHandler;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.PostVentaRestMapper;
import com.ticketseller.infrastructure.adapter.in.rest.postventa.dto.CambiarEstadoTicketRequest;
import com.ticketseller.infrastructure.adapter.in.rest.postventa.dto.TicketConReembolsoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = AdminTicketController.class)
@Import(GlobalExceptionHandler.class)
class AdminTicketControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private CambiarEstadoTicketUseCase cambiarEstadoTicketUseCase;

    @MockBean
    private PostVentaRestMapper postVentaRestMapper;

    @Test
    void cambiarEstadoVendidoRetorna200() {
        UUID ticketId = UUID.randomUUID();
        CambiarEstadoTicketRequest request = new CambiarEstadoTicketRequest(EstadoTicket.VENDIDO, "Validado", UUID.randomUUID());
        Ticket ticket = Ticket.builder()
                .id(ticketId)
                .ventaId(UUID.randomUUID())
                .eventoId(UUID.randomUUID())
                .zonaId(UUID.randomUUID())
                .estado(EstadoTicket.VENDIDO)
                .precio(BigDecimal.valueOf(10))
                .build();
        TicketConReembolsoResponse response = new TicketConReembolsoResponse(ticketId, EstadoTicket.VENDIDO, null, null, null);
        when(cambiarEstadoTicketUseCase.ejecutar(any(), any(), any(), any())).thenReturn(Mono.just(ticket));
        when(postVentaRestMapper.toTicketConReembolsoResponse(ticket)).thenReturn(response);

        webTestClient.patch()
                .uri("/api/v1/admin/tickets/{id}/estado", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.estadoTicket").isEqualTo("VENDIDO");
    }

    @Test
    void transicionInvalidaRetorna422() {
        UUID ticketId = UUID.randomUUID();
        CambiarEstadoTicketRequest request = new CambiarEstadoTicketRequest(EstadoTicket.VENDIDO, "Inválido", UUID.randomUUID());
        when(cambiarEstadoTicketUseCase.ejecutar(any(), any(), any(), any()))
                .thenReturn(Mono.error(new TransicionEstadoInvalidaException(EstadoTicket.REEMBOLSADO, EstadoTicket.VENDIDO)));

        webTestClient.patch()
                .uri("/api/v1/admin/tickets/{id}/estado", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(422);
    }
}
