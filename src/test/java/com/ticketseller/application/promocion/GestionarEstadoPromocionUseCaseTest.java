package com.ticketseller.application.promocion;

import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestionarEstadoPromocionUseCaseTest {

    @Mock
    private PromocionRepositoryPort promocionRepositoryPort;

    @InjectMocks
    private GestionarEstadoPromocionUseCase useCase;

    private UUID promocionId;
    private Promocion promocionActiva;
    private Promocion promocionPausada;
    private Promocion promocionFinalizada;

    @BeforeEach
    void setUp() {
        promocionId = UUID.randomUUID();

        promocionActiva = Promocion.builder()
                .id(promocionId)
                .estado(EstadoPromocion.ACTIVA)
                .build();

        promocionPausada = Promocion.builder()
                .id(promocionId)
                .estado(EstadoPromocion.PAUSADA)
                .build();

        promocionFinalizada = Promocion.builder()
                .id(promocionId)
                .estado(EstadoPromocion.FINALIZADA)
                .build();
    }

    @Test
    void debePausarPromocionActivaExitosamente() {
        when(promocionRepositoryPort.buscarPorId(promocionId)).thenReturn(Mono.just(promocionActiva));
        when(promocionRepositoryPort.actualizarEstado(eq(promocionId), eq(EstadoPromocion.PAUSADA)))
                .thenReturn(Mono.just(promocionPausada));

        StepVerifier.create(useCase.ejecutar(promocionId, EstadoPromocion.PAUSADA))
                .expectNextMatches(p -> EstadoPromocion.PAUSADA.equals(p.getEstado()))
                .verifyComplete();

        verify(promocionRepositoryPort).actualizarEstado(promocionId, EstadoPromocion.PAUSADA);
    }

    @Test
    void debeReactivarPromocionPausadaExitosamente() {
        when(promocionRepositoryPort.buscarPorId(promocionId)).thenReturn(Mono.just(promocionPausada));
        when(promocionRepositoryPort.actualizarEstado(eq(promocionId), eq(EstadoPromocion.ACTIVA)))
                .thenReturn(Mono.just(promocionActiva));

        StepVerifier.create(useCase.ejecutar(promocionId, EstadoPromocion.ACTIVA))
                .expectNextMatches(p -> EstadoPromocion.ACTIVA.equals(p.getEstado()))
                .verifyComplete();
    }

    @Test
    void debeFinalizarPromocionActivaExitosamente() {
        when(promocionRepositoryPort.buscarPorId(promocionId)).thenReturn(Mono.just(promocionActiva));
        when(promocionRepositoryPort.actualizarEstado(eq(promocionId), eq(EstadoPromocion.FINALIZADA)))
                .thenReturn(Mono.just(promocionFinalizada));

        StepVerifier.create(useCase.ejecutar(promocionId, EstadoPromocion.FINALIZADA))
                .expectNextMatches(p -> EstadoPromocion.FINALIZADA.equals(p.getEstado()))
                .verifyComplete();
    }

    @Test
    void debeLanzarErrorSiPromocionNoExiste() {
        when(promocionRepositoryPort.buscarPorId(promocionId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(promocionId, EstadoPromocion.PAUSADA))
                .expectErrorMatches(t -> t instanceof IllegalArgumentException && t.getMessage().equals("Promocion no encontrada"))
                .verify();
    }

    @Test
    void debeLanzarErrorAlIntentarReactivarPromocionFinalizada() {
        when(promocionRepositoryPort.buscarPorId(promocionId)).thenReturn(Mono.just(promocionFinalizada));

        StepVerifier.create(useCase.ejecutar(promocionId, EstadoPromocion.ACTIVA))
                .expectErrorMatches(t -> t instanceof IllegalArgumentException && t.getMessage().equals("Una promocion finalizada no puede reactivarse"))
                .verify();

        verify(promocionRepositoryPort).buscarPorId(promocionId);
        // No se debe llamar a actualizar estado
    }

    @Test
    void debeLanzarErrorSiArgumentosSonNulos() {
        StepVerifier.create(useCase.ejecutar(null, EstadoPromocion.ACTIVA))
                .expectError(IllegalArgumentException.class)
                .verify();

        StepVerifier.create(useCase.ejecutar(promocionId, null))
                .expectError(IllegalArgumentException.class)
                .verify();

        verifyNoInteractions(promocionRepositoryPort);
    }
}
