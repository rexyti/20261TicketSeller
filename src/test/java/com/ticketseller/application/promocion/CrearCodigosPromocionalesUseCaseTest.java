package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.promocion.PromocionNoActivaException;
import com.ticketseller.domain.model.promocion.CodigoPromocional;
import com.ticketseller.domain.model.promocion.EstadoCodigoPromocional;
import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.repository.CodigoPromocionalRepositoryPort;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrearCodigosPromocionalesUseCaseTest {

    @Mock
    private CodigoPromocionalRepositoryPort codigoPromocionalRepositoryPort;

    @Mock
    private PromocionRepositoryPort promocionRepositoryPort;

    @InjectMocks
    private CrearCodigosPromocionalesUseCase useCase;

    private CrearCodigosPromocionalesCommand command;
    private Promocion promocionActiva;

    @BeforeEach
    void setUp() {
        command = new CrearCodigosPromocionalesCommand(
                UUID.randomUUID(),
                10,
                1,
                "TEST",
                LocalDateTime.now().plusDays(5)
        );

        promocionActiva = Promocion.builder()
                .id(command.promocionId())
                .estado(EstadoPromocion.ACTIVA)
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .fechaFin(LocalDateTime.now().plusDays(10))
                .build();
    }

    @Test
    void debeCrearCodigosExitosamente() {
        when(promocionRepositoryPort.buscarPorId(command.promocionId())).thenReturn(Mono.just(promocionActiva));
        when(codigoPromocionalRepositoryPort.guardarTodos(anyList())).thenAnswer(invocation -> {
            List<CodigoPromocional> codigos = invocation.getArgument(0);
            return Flux.fromIterable(codigos);
        });

        StepVerifier.create(useCase.ejecutar(command))
                .expectNextCount(10)
                .verifyComplete();

        verify(promocionRepositoryPort).buscarPorId(command.promocionId());
        verify(codigoPromocionalRepositoryPort).guardarTodos(anyList());
    }

    @Test
    void debeLanzarErrorSiPromocionNoEstaActiva() {
        Promocion promocionPausada = Promocion.builder()
                .id(command.promocionId())
                .estado(EstadoPromocion.PAUSADA)
                .build();

        when(promocionRepositoryPort.buscarPorId(command.promocionId())).thenReturn(Mono.just(promocionPausada));

        StepVerifier.create(useCase.ejecutar(command))
                .expectError(PromocionNoActivaException.class)
                .verify();

        verifyNoInteractions(codigoPromocionalRepositoryPort);
    }

    @Test
    void debeLanzarErrorSiCantidadEsMenorAUno() {
        CrearCodigosPromocionalesCommand cmdMalo = new CrearCodigosPromocionalesCommand(
                command.promocionId(), 0, 1, "TEST", LocalDateTime.now().plusDays(5)
        );

        when(promocionRepositoryPort.buscarPorId(command.promocionId())).thenReturn(Mono.just(promocionActiva));

        StepVerifier.create(useCase.ejecutar(cmdMalo))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().equals("cantidad debe ser mayor a 0"))
                .verify();
    }

    @Test
    void debeLanzarErrorSiFechaFinEsAnteriorAFechaInicioPromocion() {
        CrearCodigosPromocionalesCommand cmdMalo = new CrearCodigosPromocionalesCommand(
                command.promocionId(), 10, 1, "TEST", promocionActiva.getFechaInicio().minusDays(1)
        );

        when(promocionRepositoryPort.buscarPorId(command.promocionId())).thenReturn(Mono.just(promocionActiva));

        StepVerifier.create(useCase.ejecutar(cmdMalo))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().equals("fechaFin debe ser posterior o igual a fechaInicio de la promocion"))
                .verify();
    }
}
