package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.promocion.PromocionNoActivaException;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.promocion.Descuento;
import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.model.promocion.TipoDescuento;
import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.domain.repository.DescuentoRepositoryPort;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrearDescuentoUseCaseTest {

    @Mock
    private DescuentoRepositoryPort descuentoRepositoryPort;

    @Mock
    private PromocionRepositoryPort promocionRepositoryPort;

    @Mock
    private ZonaRepositoryPort zonaRepositoryPort;

    @Mock
    private EventoRepositoryPort eventoRepositoryPort;

    @InjectMocks
    private CrearDescuentoUseCase useCase;

    private CrearDescuentoCommand command;
    private Promocion promocionActiva;
    private Promocion promocionPausada;
    private Evento evento;
    private Zona zona;
    private Descuento descuentoGuardado;

    @BeforeEach
    void setUp() {
        command = new CrearDescuentoCommand(
                UUID.randomUUID(),
                TipoDescuento.PORCENTAJE,
                BigDecimal.valueOf(20),
                UUID.randomUUID(),
                true
        );

        UUID recintoId = UUID.randomUUID();

        promocionActiva = Promocion.builder()
                .id(command.promocionId())
                .eventoId(UUID.randomUUID())
                .estado(EstadoPromocion.ACTIVA)
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .fechaFin(LocalDateTime.now().plusDays(1))
                .build();

        promocionPausada = Promocion.builder()
                .id(command.promocionId())
                .eventoId(promocionActiva.getEventoId())
                .estado(EstadoPromocion.PAUSADA)
                .build();

        evento = Evento.builder()
                .id(promocionActiva.getEventoId())
                .recintoId(recintoId)
                .build();

        zona = Zona.builder()
                .id(command.zonaId())
                .recintoId(recintoId)
                .build();

        descuentoGuardado = Descuento.builder()
                .id(UUID.randomUUID())
                .promocionId(command.promocionId())
                .tipo(command.tipo())
                .valor(command.valor())
                .zonaId(command.zonaId())
                .acumulable(command.acumulable())
                .build();
    }

    @Test
    void debeCrearDescuentoExitosamenteConZonaValida() {
        when(promocionRepositoryPort.buscarPorId(command.promocionId())).thenReturn(Mono.just(promocionActiva));
        when(eventoRepositoryPort.buscarPorId(promocionActiva.getEventoId())).thenReturn(Mono.just(evento));
        when(zonaRepositoryPort.buscarPorId(command.zonaId())).thenReturn(Mono.just(zona));
        when(descuentoRepositoryPort.guardar(any(Descuento.class))).thenReturn(Mono.just(descuentoGuardado));

        StepVerifier.create(useCase.ejecutar(command))
                .expectNextMatches(d -> d.getId().equals(descuentoGuardado.getId()))
                .verifyComplete();

        verify(promocionRepositoryPort).buscarPorId(command.promocionId());
        verify(descuentoRepositoryPort).guardar(any(Descuento.class));
    }

    @Test
    void debeCrearDescuentoExitosamenteSinZona() {
        CrearDescuentoCommand cmdSinZona = new CrearDescuentoCommand(
                command.promocionId(), TipoDescuento.MONTO_FIJO, BigDecimal.valueOf(50), null, false
        );

        when(promocionRepositoryPort.buscarPorId(cmdSinZona.promocionId())).thenReturn(Mono.just(promocionActiva));
        when(descuentoRepositoryPort.guardar(any(Descuento.class))).thenReturn(Mono.just(descuentoGuardado));

        StepVerifier.create(useCase.ejecutar(cmdSinZona))
                .expectNextCount(1)
                .verifyComplete();

        verifyNoInteractions(eventoRepositoryPort, zonaRepositoryPort);
    }

    @Test
    void debeLanzarErrorSiPromocionNoEstaActiva() {
        when(promocionRepositoryPort.buscarPorId(command.promocionId())).thenReturn(Mono.just(promocionPausada));

        StepVerifier.create(useCase.ejecutar(command))
                .expectError(PromocionNoActivaException.class)
                .verify();

        verifyNoInteractions(descuentoRepositoryPort);
    }

    @Test
    void debeLanzarErrorSiZonaNoPerteneceAlEvento() {
        Zona zonaOtroRecinto = Zona.builder()
                .id(command.zonaId())
                .recintoId(UUID.randomUUID()) // Distinto al recinto del evento
                .build();

        when(promocionRepositoryPort.buscarPorId(command.promocionId())).thenReturn(Mono.just(promocionActiva));
        when(eventoRepositoryPort.buscarPorId(promocionActiva.getEventoId())).thenReturn(Mono.just(evento));
        when(zonaRepositoryPort.buscarPorId(command.zonaId())).thenReturn(Mono.just(zonaOtroRecinto));

        StepVerifier.create(useCase.ejecutar(command))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().equals("La zona no pertenece al evento de la promocion"))
                .verify();

        verifyNoInteractions(descuentoRepositoryPort);
    }
}
