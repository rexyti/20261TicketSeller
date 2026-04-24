package com.ticketseller.application.checkout;

import com.ticketseller.domain.exception.PagoRechazadoException;
import com.ticketseller.domain.model.EstadoTicket;
import com.ticketseller.domain.model.EstadoVenta;
import com.ticketseller.domain.model.ResultadoPago;
import com.ticketseller.domain.model.Ticket;
import com.ticketseller.domain.model.Venta;
import com.ticketseller.domain.repository.CodigoQrPort;
import com.ticketseller.domain.repository.NotificacionEmailPort;
import com.ticketseller.domain.repository.PasarelaPagoPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.TransaccionFinancieraRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcesarPagoUseCaseTest {

    @Test
    void deberiaCompletarPagoExitoso() {
        VentaRepositoryPort ventaRepositoryPort = mock(VentaRepositoryPort.class);
        TicketRepositoryPort ticketRepositoryPort = mock(TicketRepositoryPort.class);
        TransaccionFinancieraRepositoryPort transaccionRepository = mock(TransaccionFinancieraRepositoryPort.class);
        PasarelaPagoPort pasarelaPagoPort = mock(PasarelaPagoPort.class);
        NotificacionEmailPort notificacionEmailPort = mock(NotificacionEmailPort.class);
        CodigoQrPort codigoQrPort = mock(CodigoQrPort.class);

        ProcesarPagoUseCase useCase = new ProcesarPagoUseCase(ventaRepositoryPort, ticketRepositoryPort,
                transaccionRepository, pasarelaPagoPort, notificacionEmailPort, codigoQrPort);

        UUID ventaId = UUID.randomUUID();
        Venta venta = Venta.builder()
                .id(ventaId)
                .estado(EstadoVenta.RESERVADA)
                .total(BigDecimal.valueOf(100))
                .fechaExpiracion(LocalDateTime.now().plusMinutes(5))
                .build();

        Ticket ticket = Ticket.builder().id(UUID.randomUUID()).ventaId(ventaId)
                .eventoId(UUID.randomUUID()).zonaId(UUID.randomUUID()).precio(BigDecimal.TEN).build();

        when(ventaRepositoryPort.buscarPorId(ventaId)).thenReturn(Mono.just(venta));
        when(pasarelaPagoPort.procesarPago(any(), any(), any())).thenReturn(Mono.just(new ResultadoPago(true, "APROBADO", "AUTH", "OK")));
        when(ticketRepositoryPort.buscarPorVenta(ventaId)).thenReturn(Flux.fromIterable(List.of(ticket)));
        when(ticketRepositoryPort.guardarTodos(any())).thenAnswer(invocation -> Flux.fromIterable(invocation.getArgument(0)));
        when(ventaRepositoryPort.actualizarEstado(ventaId, EstadoVenta.COMPLETADA)).thenReturn(Mono.just(venta.toBuilder().estado(EstadoVenta.COMPLETADA).build()));
        when(transaccionRepository.guardar(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(notificacionEmailPort.enviarConfirmacion(any(), any())).thenReturn(Mono.empty());
        when(codigoQrPort.generarCodigo(any())).thenReturn("qr");

        StepVerifier.create(useCase.ejecutar(ventaId, new ProcesarPagoCommand("TARJETA", "127.0.0.1")))
                .assertNext(detalle -> {
                    assert detalle.venta().getEstado() == EstadoVenta.COMPLETADA;
                })
                .verifyComplete();
    }

    @Test
    void deberiaFallarSiElPagoEsRechazado() {
        VentaRepositoryPort ventaRepositoryPort = mock(VentaRepositoryPort.class);
        TicketRepositoryPort ticketRepositoryPort = mock(TicketRepositoryPort.class);
        TransaccionFinancieraRepositoryPort transaccionRepository = mock(TransaccionFinancieraRepositoryPort.class);
        PasarelaPagoPort pasarelaPagoPort = mock(PasarelaPagoPort.class);
        NotificacionEmailPort notificacionEmailPort = mock(NotificacionEmailPort.class);
        CodigoQrPort codigoQrPort = mock(CodigoQrPort.class);

        ProcesarPagoUseCase useCase = new ProcesarPagoUseCase(ventaRepositoryPort, ticketRepositoryPort,
                transaccionRepository, pasarelaPagoPort, notificacionEmailPort, codigoQrPort);

        UUID ventaId = UUID.randomUUID();
        Venta venta = Venta.builder()
                .id(ventaId)
                .estado(EstadoVenta.RESERVADA)
                .total(BigDecimal.valueOf(100))
                .fechaExpiracion(LocalDateTime.now().plusMinutes(5))
                .build();

        when(ventaRepositoryPort.buscarPorId(ventaId)).thenReturn(Mono.just(venta));
        when(pasarelaPagoPort.procesarPago(any(), any(), any()))
                .thenReturn(Mono.just(new ResultadoPago(false, "RECHAZADO", null, "Sin fondos")));
        when(transaccionRepository.guardar(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(ventaId, new ProcesarPagoCommand("TARJETA", "127.0.0.1")))
                .expectError(PagoRechazadoException.class)
                .verify();
    }
}
