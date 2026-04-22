package com.ticketseller.application.capacidad;

import com.ticketseller.domain.model.CategoriaRecinto;
import com.ticketseller.domain.model.Recinto;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigurarCategoriaUseCaseTest {

    @Test
    void deberiaConfigurarCategoria() {
        UUID id = UUID.randomUUID();
        RecintoRepositoryPort port = mock(RecintoRepositoryPort.class);
        ConfigurarCategoriaUseCase useCase = new ConfigurarCategoriaUseCase(port);
        Recinto actual = Recinto.builder().id(id).categoria(null).build();

        when(port.buscarPorId(id)).thenReturn(Mono.just(actual));
        when(port.guardar(any(Recinto.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(id, CategoriaRecinto.TEATRO))
                .expectNextMatches(r -> r.getCategoria() == CategoriaRecinto.TEATRO)
                .verifyComplete();
    }
}

