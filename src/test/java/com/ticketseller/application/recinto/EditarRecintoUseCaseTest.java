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

class EditarRecintoUseCaseTest {

    @Test
    void deberiaEditarCamposDescriptivos() {
        UUID id = UUID.randomUUID();
        Recinto actual = Recinto.builder().id(id).nombre("A").ciudad("Bogota").direccion("Old").capacidadMaxima(1000).activo(true).build();
        RecintoRepositoryPort port = mock(RecintoRepositoryPort.class);
        EditarRecintoUseCase useCase = new EditarRecintoUseCase(port);

        when(port.buscarPorId(id)).thenReturn(Mono.just(actual));
        when(port.guardar(any(Recinto.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(id, Recinto.builder().direccion("New").build()))
                .expectNextMatches(r -> "New".equals(r.getDireccion()))
                .verifyComplete();
    }

    @Test
    void deberiaBloquearCambioEstructuralConTicketsVendidos() {
        UUID id = UUID.randomUUID();
        Recinto actual = Recinto.builder().id(id).capacidadMaxima(1000).activo(true).build();
        RecintoRepositoryPort port = mock(RecintoRepositoryPort.class);
        EditarRecintoUseCase useCase = new EditarRecintoUseCase(port);

        when(port.buscarPorId(id)).thenReturn(Mono.just(actual));
        when(port.tieneTicketsVendidos(id)).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.ejecutar(id, Recinto.builder().capacidadMaxima(1200).build()))
                .expectError(RecintoConEventosException.class)
                .verify();
    }
}

