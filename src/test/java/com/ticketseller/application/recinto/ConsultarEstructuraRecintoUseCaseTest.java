package com.ticketseller.application.recinto;

import com.ticketseller.domain.exception.recinto.RecintoNotFoundException;
import com.ticketseller.domain.model.recinto.Recinto;
import com.ticketseller.domain.model.zona.Compuerta;
import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.domain.repository.CompuertaRepositoryPort;
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
    @Mock
    private CompuertaRepositoryPort compuertaRepositoryPort;

    private ConsultarEstructuraRecintoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarEstructuraRecintoUseCase(recintoRepositoryPort, zonaRepositoryPort, compuertaRepositoryPort);
    }

    @Test
    void debeRetornarEstructuraCuandoRecintoExiste() {
        UUID recintoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();
        Recinto recinto = Recinto.builder().id(recintoId).nombre("Estadio").build();
        Zona zona = Zona.builder().id(zonaId).recintoId(recintoId).nombre("Zona A").build();
        Compuerta compuerta = Compuerta.builder().id(UUID.randomUUID()).recintoId(recintoId).zonaId(zonaId).nombre("Puerta 1").build();

        when(recintoRepositoryPort.buscarPorId(recintoId)).thenReturn(Mono.just(recinto));
        when(zonaRepositoryPort.buscarPorRecintoId(recintoId)).thenReturn(Flux.just(zona));
        when(compuertaRepositoryPort.buscarPorRecintoId(recintoId)).thenReturn(Flux.just(compuerta));

        StepVerifier.create(useCase.ejecutar(recintoId))
                .expectNextMatches(tuple ->
                        tuple.getT1().getId().equals(recintoId)
                                && tuple.getT2().size() == 1
                                && tuple.getT2().getFirst().getNombre().equals("Zona A")
                                && tuple.getT3().size() == 1
                                && tuple.getT3().getFirst().getNombre().equals("Puerta 1")
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
