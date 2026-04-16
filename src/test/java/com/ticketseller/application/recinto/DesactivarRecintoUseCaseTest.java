package com.ticketseller.application.recinto;

import com.ticketseller.domain.exception.RecintoConEventosException;
import com.ticketseller.domain.model.Recinto;
import com.ticketseller.domain.port.out.RecintoRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DesactivarRecintoUseCaseTest {

    @Test
    void deberiaDesactivarRecinto() {
        UUID id = UUID.randomUUID();
        RecintoRepositoryPort port = mock(RecintoRepositoryPort.class);
        DesactivarRecintoUseCase useCase = new DesactivarRecintoUseCase(port);
        Recinto actual = Recinto.builder().id(id).activo(true).build();

        when(port.buscarPorId(id)).thenReturn(Mono.just(actual));
        when(port.tieneEventosFuturos(id)).thenReturn(Mono.just(false));
        when(port.guardar(any(Recinto.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(id))
                .expectNextMatches(r -> !r.isActivo())
                .verifyComplete();
    }

    @Test
    void deberiaBloquearSiTieneEventos() {
        UUID id = UUID.randomUUID();
        RecintoRepositoryPort port = mock(RecintoRepositoryPort.class);
        DesactivarRecintoUseCase useCase = new DesactivarRecintoUseCase(port);
        Recinto actual = Recinto.builder().id(id).activo(true).build();

        when(port.buscarPorId(id)).thenReturn(Mono.just(actual));
        when(port.tieneEventosFuturos(id)).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.ejecutar(id))
                .expectError(RecintoConEventosException.class)
                .verify();
    }
}

