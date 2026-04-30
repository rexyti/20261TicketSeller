package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.evento.EventoNotFoundException;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.model.promocion.TipoPromocion;
import com.ticketseller.domain.model.promocion.TipoUsuario;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrearPromocionUseCaseTest {

    @Mock
    private PromocionRepositoryPort promocionRepositoryPort;

    @Mock
    private EventoRepositoryPort eventoRepositoryPort;

    @InjectMocks
    private CrearPromocionUseCase useCase;

    private CrearPromocionCommand command;
    private Evento evento;
    private Promocion promocionGuardada;

    @BeforeEach
    void setUp() {
        command = new CrearPromocionCommand(
                "Preventa VIP",
                TipoPromocion.PREVENTA,
                UUID.randomUUID(),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(5),
                TipoUsuario.VIP
        );

        evento = Evento.builder()
                .id(command.eventoId())
                .nombre("Concierto Test")
                .build();

        promocionGuardada = Promocion.builder()
                .id(UUID.randomUUID())
                .nombre(command.nombre())
                .tipo(command.tipo())
                .eventoId(command.eventoId())
                .fechaInicio(command.fechaInicio())
                .fechaFin(command.fechaFin())
                .tipoUsuarioRestringido(command.tipoUsuarioRestringido())
                .build();
    }

    @Test
    void debeCrearPromocionExitosamente() {
        when(eventoRepositoryPort.buscarPorId(command.eventoId())).thenReturn(Mono.just(evento));
        when(promocionRepositoryPort.guardar(any(Promocion.class))).thenReturn(Mono.just(promocionGuardada));

        StepVerifier.create(useCase.ejecutar(command))
                .expectNextMatches(promocion -> promocion.getId().equals(promocionGuardada.getId()))
                .verifyComplete();

        verify(eventoRepositoryPort).buscarPorId(command.eventoId());
        verify(promocionRepositoryPort).guardar(any(Promocion.class));
    }

    @Test
    void debeLanzarErrorSiFechasSonNulas() {
        CrearPromocionCommand cmdMalo = new CrearPromocionCommand(
                "Promo", TipoPromocion.PREVENTA, UUID.randomUUID(), null, null, null
        );

        StepVerifier.create(useCase.ejecutar(cmdMalo))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().equals("fechaInicio y fechaFin son obligatorias"))
                .verify();
    }

    @Test
    void debeLanzarErrorSiFechaFinEsAnteriorAFechaInicio() {
        CrearPromocionCommand cmdMalo = new CrearPromocionCommand(
                "Promo", TipoPromocion.PREVENTA, UUID.randomUUID(),
                LocalDateTime.now().plusDays(5), LocalDateTime.now().plusDays(1), null
        );

        StepVerifier.create(useCase.ejecutar(cmdMalo))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().equals("fechaFin debe ser posterior o igual a fechaInicio"))
                .verify();
    }

    @Test
    void debeLanzarErrorSiEventoNoExiste() {
        when(eventoRepositoryPort.buscarPorId(command.eventoId())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(command))
                .expectError(EventoNotFoundException.class)
                .verify();

        verify(eventoRepositoryPort).buscarPorId(command.eventoId());
    }
}
