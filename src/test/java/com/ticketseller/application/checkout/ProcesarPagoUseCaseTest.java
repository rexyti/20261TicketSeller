package com.ticketseller.application.checkout;

import com.ticketseller.domain.exception.venta.PagoRechazadoException;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.venta.EstadoVenta;
import com.ticketseller.domain.model.venta.ResultadoPago;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.model.zona.Compuerta;
import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.domain.repository.AsientoHoldRepositoryPort;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.CodigoQrPort;
import com.ticketseller.domain.repository.CompuertaRepositoryPort;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.NotificacionEmailPort;
import com.ticketseller.domain.repository.PasarelaPagoPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.TransaccionFinancieraRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import org.junit.jupiter.api.Assertions;
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

    private ProcesarPagoUseCase buildUseCase(VentaRepositoryPort ventaRepositoryPort,
                                              TicketRepositoryPort ticketRepositoryPort,
                                              TransaccionFinancieraRepositoryPort transaccionRepository,
                                              PasarelaPagoPort pasarelaPagoPort,
                                              NotificacionEmailPort notificacionEmailPort,
                                              CodigoQrPort codigoQrPort,
                                              AsientoRepositoryPort asientoRepositoryPort,
                                              AsientoHoldRepositoryPort asientoHoldRepositoryPort,
                                              ZonaRepositoryPort zonaRepositoryPort,
                                              CompuertaRepositoryPort compuertaRepositoryPort,
                                              EventoRepositoryPort eventoRepositoryPort) {
        return new ProcesarPagoUseCase(ventaRepositoryPort, ticketRepositoryPort, transaccionRepository,
                pasarelaPagoPort, notificacionEmailPort, codigoQrPort, asientoRepositoryPort,
                asientoHoldRepositoryPort, zonaRepositoryPort, compuertaRepositoryPort, eventoRepositoryPort);
    }

    @Test
    void deberiaCompletarPagoExitoso() {
        VentaRepositoryPort ventaRepositoryPort = mock(VentaRepositoryPort.class);
        TicketRepositoryPort ticketRepositoryPort = mock(TicketRepositoryPort.class);
        TransaccionFinancieraRepositoryPort transaccionRepository = mock(TransaccionFinancieraRepositoryPort.class);
        PasarelaPagoPort pasarelaPagoPort = mock(PasarelaPagoPort.class);
        NotificacionEmailPort notificacionEmailPort = mock(NotificacionEmailPort.class);
        CodigoQrPort codigoQrPort = mock(CodigoQrPort.class);
        AsientoRepositoryPort asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        AsientoHoldRepositoryPort asientoHoldRepositoryPort = mock(AsientoHoldRepositoryPort.class);
        ZonaRepositoryPort zonaRepositoryPort = mock(ZonaRepositoryPort.class);
        CompuertaRepositoryPort compuertaRepositoryPort = mock(CompuertaRepositoryPort.class);
        EventoRepositoryPort eventoRepositoryPort = mock(EventoRepositoryPort.class);

        ProcesarPagoUseCase useCase = buildUseCase(ventaRepositoryPort, ticketRepositoryPort,
                transaccionRepository, pasarelaPagoPort, notificacionEmailPort, codigoQrPort,
                asientoRepositoryPort, asientoHoldRepositoryPort, zonaRepositoryPort,
                compuertaRepositoryPort, eventoRepositoryPort);

        UUID ventaId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();

        Venta venta = Venta.builder()
                .id(ventaId)
                .eventoId(eventoId)
                .zonaId(zonaId)
                .cantidad(1)
                .esCortesia(false)
                .estado(EstadoVenta.RESERVADA)
                .total(BigDecimal.valueOf(100))
                .fechaExpiracion(LocalDateTime.now().plusMinutes(5))
                .build();

        when(ventaRepositoryPort.buscarPorId(ventaId)).thenReturn(Mono.just(venta));
        when(pasarelaPagoPort.procesarPago(any(), any(), any()))
                .thenReturn(Mono.just(new ResultadoPago(true, "APROBADO", "AUTH", "OK")));
        when(zonaRepositoryPort.buscarPorId(zonaId)).thenReturn(Mono.just(
                Zona.builder().id(zonaId).nombre("Zona A").build()));
        when(compuertaRepositoryPort.buscarPorZonaId(zonaId)).thenReturn(Flux.just(
                Compuerta.builder().id(UUID.randomUUID()).nombre("Norte").esGeneral(true).build()));
        when(eventoRepositoryPort.buscarPorId(eventoId)).thenReturn(Mono.just(
                Evento.builder().id(eventoId).fechaInicio(LocalDateTime.now().plusDays(30)).build()));
        when(asientoHoldRepositoryPort.buscarPorVentaId(ventaId)).thenReturn(Flux.empty());
        when(codigoQrPort.generarCodigo(any())).thenReturn("qr-code");
        when(ticketRepositoryPort.guardarTodos(any()))
                .thenAnswer(invocation -> Flux.fromIterable(invocation.getArgument(0)));
        when(ventaRepositoryPort.actualizarEstado(ventaId, EstadoVenta.COMPLETADA))
                .thenReturn(Mono.just(venta.toBuilder().estado(EstadoVenta.COMPLETADA).build()));
        when(transaccionRepository.guardar(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(notificacionEmailPort.enviarConfirmacion(any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(ventaId, new ProcesarPagoCommand("TARJETA", "127.0.0.1")))
                .assertNext(detalle -> {
                    Assertions.assertEquals(EstadoVenta.COMPLETADA, detalle.venta().getEstado());
                    Assertions.assertEquals(1, detalle.tickets().size());
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
        AsientoRepositoryPort asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        AsientoHoldRepositoryPort asientoHoldRepositoryPort = mock(AsientoHoldRepositoryPort.class);
        ZonaRepositoryPort zonaRepositoryPort = mock(ZonaRepositoryPort.class);
        CompuertaRepositoryPort compuertaRepositoryPort = mock(CompuertaRepositoryPort.class);
        EventoRepositoryPort eventoRepositoryPort = mock(EventoRepositoryPort.class);

        ProcesarPagoUseCase useCase = buildUseCase(ventaRepositoryPort, ticketRepositoryPort,
                transaccionRepository, pasarelaPagoPort, notificacionEmailPort, codigoQrPort,
                asientoRepositoryPort, asientoHoldRepositoryPort, zonaRepositoryPort,
                compuertaRepositoryPort, eventoRepositoryPort);

        UUID ventaId = UUID.randomUUID();
        Venta venta = Venta.builder()
                .id(ventaId)
                .eventoId(UUID.randomUUID())
                .zonaId(UUID.randomUUID())
                .cantidad(1)
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
