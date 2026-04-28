package com.ticketseller.application;

import com.ticketseller.domain.exception.recinto.RecintoNotFoundException;
import com.ticketseller.domain.model.recinto.Recinto;
import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarEstructuraRecintoUseCaseTest {

    @Mock
    private RecintoRepositoryPort recintoRepositoryPort;
    @Mock
    private ZonaRepositoryPort zonaRepositoryPort;

    private ConsultarEstructuraRecintoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarEstructuraRecintoUseCase(recintoRepositoryPort, zonaRepositoryPort);
    }

    @Test
    void debeRetornarEstructuraCuandoRecintoExiste() {
        UUID recintoId = UUID.randomUUID();
        Recinto recinto = Recinto.builder().id(recintoId).nombre("Estadio").build();
        Zona zona = Zona.builder().id(UUID.randomUUID()).recintoId(recintoId).nombre("Zona A").build();

        when(recintoRepositoryPort.buscarPorId(recintoId)).thenReturn(Mono.just(recinto));
        when(zonaRepositoryPort.buscarPorRecintoId(recintoId)).thenReturn(Flux.just(zona));

        StepVerifier.create(useCase.ejecutar(recintoId))
                .expectNextMatches(response -> 
                        response.recintoId().equals(recintoId) &&
                        response.bloques().size() == 1 &&
                        response.bloques().get(0).nombre().equals("Zona A")
                )
                .verifyComplete();
    }

    @Test
    void debeLanzarExcepcionCuandoRecintoNoExiste() {
        UUID recintoId = UUID.randomUUID();
        when(recintoRepositoryPort.buscarPorId(recintoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(recintoId))
                .expectError(RecintoNotFoundException.class)
                .verify();
    }
}
