package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.checkout.ConsultarVentaUseCase;
import com.ticketseller.application.checkout.ProcesarPagoCommand;
import com.ticketseller.application.checkout.ProcesarPagoUseCase;
import com.ticketseller.application.checkout.ReservarAsientosCommand;
import com.ticketseller.application.checkout.ReservarAsientosUseCase;
import com.ticketseller.application.checkout.VentaDetalle;
import com.ticketseller.domain.exception.AsientoNoDisponibleException;
import com.ticketseller.domain.exception.PagoRechazadoException;
import com.ticketseller.domain.model.EstadoTicket;
import com.ticketseller.domain.model.EstadoVenta;
import com.ticketseller.domain.model.Ticket;
import com.ticketseller.domain.model.Venta;
import com.ticketseller.infrastructure.adapter.in.rest.dto.checkout.ProcesarPagoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.checkout.ReservarAsientosRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.checkout.TicketResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.checkout.VentaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.CheckoutRestMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = CheckoutController.class)
@Import(GlobalExceptionHandler.class)
class CheckoutControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ReservarAsientosUseCase reservarAsientosUseCase;

    @MockBean
    private ProcesarPagoUseCase procesarPagoUseCase;

    @MockBean
    private ConsultarVentaUseCase consultarVentaUseCase;

    @MockBean
    private CheckoutRestMapper checkoutRestMapper;

    @Test
    void reservarDisponibleRetorna201() {
        UUID ventaId = UUID.randomUUID();
        UUID compradorId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        UUID compuertaId = UUID.randomUUID();

        ReservarAsientosRequest request = new ReservarAsientosRequest(compradorId, eventoId, zonaId, 1, false);
        ReservarAsientosCommand command = new ReservarAsientosCommand(compradorId, eventoId, zonaId, 1, false);

        Venta venta = Venta.builder()
                .id(ventaId)
                .compradorId(compradorId)
                .eventoId(eventoId)
                .estado(EstadoVenta.RESERVADA)
                .fechaCreacion(LocalDateTime.now())
                .fechaExpiracion(LocalDateTime.now().plusMinutes(15))
                .total(BigDecimal.valueOf(75))
                .build();
        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .zonaId(zonaId)
                .compuertaId(compuertaId)
                .precio(BigDecimal.valueOf(75))
                .esCortesia(false)
                .build();
        VentaDetalle detalle = new VentaDetalle(venta, List.of(ticket));

        VentaResponse response = new VentaResponse(
                ventaId,
                compradorId,
                eventoId,
                EstadoVenta.RESERVADA,
                venta.getFechaCreacion(),
                venta.getFechaExpiracion(),
                venta.getTotal(),
                List.of(new TicketResponse(ticket.getId(), zonaId, compuertaId, null,
                        ticket.getPrecio(), null, false))
        );

        when(checkoutRestMapper.toCommand(request)).thenReturn(command);
        when(reservarAsientosUseCase.ejecutar(command)).thenReturn(Mono.just(detalle));
        when(checkoutRestMapper.toResponse(detalle)).thenReturn(response);

        webTestClient.post()
                .uri("/api/v1/checkout/reservar")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("RESERVADA");
    }

    @Test
    void reservarNoDisponibleRetorna409() {
        ReservarAsientosRequest request = new ReservarAsientosRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1, false);
        when(checkoutRestMapper.toCommand(any(ReservarAsientosRequest.class)))
                .thenReturn(new ReservarAsientosCommand(request.compradorId(), request.eventoId(), request.zonaId(), request.cantidad(), false));
        when(reservarAsientosUseCase.ejecutar(any())).thenReturn(Mono.error(new AsientoNoDisponibleException("ocupado")));

        webTestClient.post()
                .uri("/api/v1/checkout/reservar")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void pagarRechazadoRetorna402() {
        UUID ventaId = UUID.randomUUID();
        ProcesarPagoRequest request = new ProcesarPagoRequest("FONDOS_INSUFICIENTES", "127.0.0.1");
        when(checkoutRestMapper.toCommand(request)).thenReturn(new ProcesarPagoCommand(request.metodoPago(), request.ip()));
        when(procesarPagoUseCase.ejecutar(any(), any())).thenReturn(Mono.error(new PagoRechazadoException("Sin fondos")));

        webTestClient.post()
                .uri("/api/v1/checkout/{ventaId}/pagar", ventaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(402);
    }
}
