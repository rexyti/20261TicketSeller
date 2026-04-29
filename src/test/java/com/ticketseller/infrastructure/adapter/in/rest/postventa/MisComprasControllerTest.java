package com.ticketseller.infrastructure.adapter.in.rest.postventa;

import com.ticketseller.application.postventa.ConsultarEstadoReembolsoUseCase;
import com.ticketseller.application.postventa.TicketConReembolso;
import com.ticketseller.domain.model.postventa.EstadoReembolso;
import com.ticketseller.domain.model.postventa.Reembolso;
import com.ticketseller.domain.model.ticket.EstadoTicket;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.infrastructure.adapter.in.rest.GlobalExceptionHandler;
import com.ticketseller.infrastructure.adapter.in.rest.dto.postventa.TicketConReembolsoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.PostVentaRestMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.when;

@WebFluxTest(controllers = MisComprasController.class)
@Import(GlobalExceptionHandler.class)
class MisComprasControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ConsultarEstadoReembolsoUseCase consultarEstadoReembolsoUseCase;
    @MockBean
    private PostVentaRestMapper postVentaRestMapper;

    @Test
    void misComprasRetornaEstadoReembolso() {
        UUID compradorId = UUID.randomUUID();
        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .ventaId(UUID.randomUUID())
                .eventoId(UUID.randomUUID())
                .zonaId(UUID.randomUUID())
                .estado(EstadoTicket.CANCELADO)
                .precio(BigDecimal.valueOf(100))
                .build();
        Reembolso reembolso = Reembolso.builder()
                .id(UUID.randomUUID())
                .estado(EstadoReembolso.PENDIENTE)
                .monto(BigDecimal.valueOf(100))
                .build();
        TicketConReembolso item = new TicketConReembolso(ticket, reembolso);
        TicketConReembolsoResponse response = new TicketConReembolsoResponse(ticket.getId(), ticket.getEstado(),
                reembolso.getEstado(), reembolso.getMonto(), reembolso.getId());
        when(consultarEstadoReembolsoUseCase.ejecutar(compradorId)).thenReturn(Flux.just(item));
        when(postVentaRestMapper.toTicketConReembolsoResponse(item)).thenReturn(response);

        webTestClient.get()
                .uri("/api/v1/compras/mis-compras")
                .header("X-Comprador-Id", compradorId.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].estadoReembolso").isEqualTo("PENDIENTE");
    }
}

