package com.ticketseller.application.mantenimiento;

import com.ticketseller.domain.exception.RecintoNotFoundException;
import com.ticketseller.domain.model.Recinto;
import com.ticketseller.domain.model.TipoAsiento;
import com.ticketseller.domain.model.Zona;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import com.ticketseller.domain.repository.TipoAsientoRepositoryPort;
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
    private TipoAsientoRepositoryPort tipoAsientoRepositoryPort;

    private ConsultarEstructuraRecintoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ConsultarEstructuraRecintoUseCase(recintoRepositoryPort, zonaRepositoryPort, tipoAsientoRepositoryPort);
    }

    @Test
    void debeRetornarEstructuraCuandoRecintoExiste() {
        UUID recintoId = UUID.randomUUID();
        UUID tipoAsientoId = UUID.randomUUID();
        Recinto recinto = Recinto.builder().id(recintoId).nombre("Estadio").build();
        Zona zona = Zona.builder().id(UUID.randomUUID()).recintoId(recintoId).nombre("Zona A").tipoAsientoId(tipoAsientoId).build();
        TipoAsiento tipoAsiento = TipoAsiento.builder().id(tipoAsientoId).nombre("VIP").build();

        when(recintoRepositoryPort.buscarPorId(recintoId)).thenReturn(Mono.just(recinto));
        when(zonaRepositoryPort.buscarPorRecintoId(recintoId)).thenReturn(Flux.just(zona));
        when(tipoAsientoRepositoryPort.buscarPorId(tipoAsientoId)).thenReturn(Mono.just(tipoAsiento));

        StepVerifier.create(useCase.ejecutar(recintoId))
                .expectNextMatches(response -> 
                        response.recintoId().equals(recintoId) &&
                        response.bloques().size() == 1 &&
                        response.bloques().get(0).nombre().equals("Zona A") &&
                        response.bloques().get(0).zonas().get(0).categoria().equals("VIP")
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
