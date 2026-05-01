package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.promocion.PromocionNotFoundException;
import com.ticketseller.domain.exception.promocion.TransicionPromocionInvalidaException;
import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.model.promocion.TipoPromocion;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GestionarEstadoPromocionUseCaseTest {

    private PromocionRepositoryPort promocionRepositoryPort;
    private GestionarEstadoPromocionUseCase useCase;

    @BeforeEach
    void setUp() {
        promocionRepositoryPort = mock(PromocionRepositoryPort.class);
        useCase = new GestionarEstadoPromocionUseCase(promocionRepositoryPort);
    }

    @Test
    void deberiaPausarPromocionActiva() {
        UUID id = UUID.randomUUID();
        Promocion activa = buildPromocion(id, EstadoPromocion.ACTIVA);

        when(promocionRepositoryPort.buscarPorId(id)).thenReturn(Mono.just(activa));
        when(promocionRepositoryPort.guardar(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(id, EstadoPromocion.PAUSADA))
                .assertNext(p -> assertThat(p.getEstado()).isEqualTo(EstadoPromocion.PAUSADA))
                .verifyComplete();
    }

    @Test
    void deberiaReanudarPromocionPausada() {
        UUID id = UUID.randomUUID();
        Promocion pausada = buildPromocion(id, EstadoPromocion.PAUSADA);

        when(promocionRepositoryPort.buscarPorId(id)).thenReturn(Mono.just(pausada));
        when(promocionRepositoryPort.guardar(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(id, EstadoPromocion.ACTIVA))
                .assertNext(p -> assertThat(p.getEstado()).isEqualTo(EstadoPromocion.ACTIVA))
                .verifyComplete();
    }

    @Test
    void deberiaFinalizarPromocionActiva() {
        UUID id = UUID.randomUUID();
        Promocion activa = buildPromocion(id, EstadoPromocion.ACTIVA);

        when(promocionRepositoryPort.buscarPorId(id)).thenReturn(Mono.just(activa));
        when(promocionRepositoryPort.guardar(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(id, EstadoPromocion.FINALIZADA))
                .assertNext(p -> assertThat(p.getEstado()).isEqualTo(EstadoPromocion.FINALIZADA))
                .verifyComplete();
    }

    @Test
    void deberiaFallarAlReactivarPromocionFinalizada() {
        UUID id = UUID.randomUUID();
        Promocion finalizada = buildPromocion(id, EstadoPromocion.FINALIZADA);

        when(promocionRepositoryPort.buscarPorId(id)).thenReturn(Mono.just(finalizada));

        StepVerifier.create(useCase.ejecutar(id, EstadoPromocion.ACTIVA))
                .expectError(TransicionPromocionInvalidaException.class)
                .verify();
    }

    @Test
    void deberiaFallarConTransicionInvalidaActivaAActiva() {
        UUID id = UUID.randomUUID();
        Promocion activa = buildPromocion(id, EstadoPromocion.ACTIVA);

        when(promocionRepositoryPort.buscarPorId(id)).thenReturn(Mono.just(activa));

        StepVerifier.create(useCase.ejecutar(id, EstadoPromocion.ACTIVA))
                .expectError(TransicionPromocionInvalidaException.class)
                .verify();
    }

    @Test
    void deberiaFallarSiPromocionNoExiste() {
        UUID id = UUID.randomUUID();
        when(promocionRepositoryPort.buscarPorId(id)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(id, EstadoPromocion.PAUSADA))
                .expectError(PromocionNotFoundException.class)
                .verify();
    }

    private Promocion buildPromocion(UUID id, EstadoPromocion estado) {
        return Promocion.builder()
                .id(id)
                .nombre("Campaña Test")
                .tipo(TipoPromocion.DESCUENTO)
                .eventoId(UUID.randomUUID())
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .fechaFin(LocalDateTime.now().plusDays(7))
                .estado(estado)
                .build();
    }
}
