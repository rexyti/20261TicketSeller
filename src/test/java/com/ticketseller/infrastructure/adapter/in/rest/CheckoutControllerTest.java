package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.checkout.ConsultarVentaUseCase;
import com.ticketseller.application.checkout.ProcesarPagoCommand;
import com.ticketseller.application.checkout.ProcesarPagoUseCase;
import com.ticketseller.application.checkout.ReservarAsientosCommand;
import com.ticketseller.application.checkout.ReservarAsientosUseCase;
import com.ticketseller.application.checkout.VentaDetalle;
import com.ticketseller.domain.exception.asiento.AsientoNoDisponibleException;
import com.ticketseller.domain.exception.venta.PagoRechazadoException;
import com.ticketseller.domain.model.venta.EstadoVenta;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.infrastructure.adapter.in.rest.checkout.CheckoutController;
import com.ticketseller.infrastructure.adapter.in.rest.checkout.dto.ProcesarPagoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.checkout.dto.ReservarAsientosRequest;
import com.ticketseller.infrastructure.adapter.in.rest.checkout.dto.TicketResponse;
import com.ticketseller.infrastructure.adapter.in.rest.checkout.dto.VentaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.CheckoutRestMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import com.ticketseller.infrastructure.config.TestWebSecurityConfig;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = CheckoutController.class)
@Import({GlobalExceptionHandler.class, TestWebSecurityConfig.class})
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

    private static final UUID COMPRADOR_ID = UUID.randomUUID();

    @Test
    void reservarDisponibleRetorna201() {
        UUID ventaId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        UUID compuertaId = UUID.randomUUID();

        ReservarAsientosRequest request = new ReservarAsientosRequest(zonaId, 1, false, null, null);
        ReservarAsientosCommand command = new ReservarAsientosCommand(COMPRADOR_ID, eventoId, zonaId, 1, false, null, null);

        Venta venta = Venta.builder()
                .id(ventaId)
                .compradorId(COMPRADOR_ID)
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
                COMPRADOR_ID,
                eventoId,
                EstadoVenta.RESERVADA,
                venta.getFechaCreacion(),
                venta.getFechaExpiracion(),
                venta.getTotal(),
                List.of(new TicketResponse(ticket.getId(), null, null, null,
                        ticket.getPrecio(), null, false, null))
        );

        when(checkoutRestMapper.toCommand(any(ReservarAsientosRequest.class), any(UUID.class), any(UUID.class))).thenReturn(command);
        when(reservarAsientosUseCase.ejecutar(command)).thenReturn(Mono.just(detalle));
        when(checkoutRestMapper.toResponse(detalle)).thenReturn(response);

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser(COMPRADOR_ID.toString()).roles("COMPRADOR"))
                .post()
                .uri("/api/v1/eventos/{eventoId}/asientos/reservar", eventoId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.estado").isEqualTo("RESERVADA");
    }

    @Test
    void reservarNoDisponibleRetorna409() {
        UUID eventoId = UUID.randomUUID();
        ReservarAsientosRequest request = new ReservarAsientosRequest(UUID.randomUUID(), 1, false, null, null);
        when(checkoutRestMapper.toCommand(any(ReservarAsientosRequest.class), any(UUID.class), any(UUID.class)))
                .thenReturn(new ReservarAsientosCommand(COMPRADOR_ID, eventoId, request.zonaId(), request.cantidad(), false, null, null));
        when(reservarAsientosUseCase.ejecutar(any())).thenReturn(Mono.error(new AsientoNoDisponibleException("ocupado")));

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser(COMPRADOR_ID.toString()).roles("COMPRADOR"))
                .post()
                .uri("/api/v1/eventos/{eventoId}/asientos/reservar", eventoId)
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

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser(COMPRADOR_ID.toString()).roles("COMPRADOR"))
                .post()
                .uri("/api/v1/checkout/{ventaId}/pagar", ventaId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(402);
    }
}
