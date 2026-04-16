package com.ticketseller.application.zona;

import com.ticketseller.domain.exception.ZonaCapacidadExcedidaException;
import com.ticketseller.domain.model.Recinto;
import com.ticketseller.domain.model.Zona;
import com.ticketseller.domain.port.out.RecintoRepositoryPort;
import com.ticketseller.domain.port.out.ZonaRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrearZonaUseCaseTest {

    @Test
    void deberiaCrearZonaValida() {
        UUID recintoId = UUID.randomUUID();
        ZonaRepositoryPort zonaPort = mock(ZonaRepositoryPort.class);
        RecintoRepositoryPort recintoPort = mock(RecintoRepositoryPort.class);
        CrearZonaUseCase useCase = new CrearZonaUseCase(zonaPort, recintoPort);

        when(recintoPort.buscarPorId(recintoId)).thenReturn(Mono.just(Recinto.builder().id(recintoId).capacidadMaxima(100).build()));
        when(zonaPort.buscarPorRecintoId(recintoId)).thenReturn(Flux.empty());
        when(zonaPort.sumarCapacidadesPorRecinto(recintoId)).thenReturn(Mono.just(20));
        when(zonaPort.guardar(any(Zona.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(recintoId, Zona.builder().nombre("VIP").capacidad(30).build()))
                .expectNextMatches(z -> z.getId() != null)
                .verifyComplete();
    }

    @Test
    void deberiaFallarSiExcedeCapacidad() {
        UUID recintoId = UUID.randomUUID();
        ZonaRepositoryPort zonaPort = mock(ZonaRepositoryPort.class);
        RecintoRepositoryPort recintoPort = mock(RecintoRepositoryPort.class);
        CrearZonaUseCase useCase = new CrearZonaUseCase(zonaPort, recintoPort);

        when(recintoPort.buscarPorId(recintoId)).thenReturn(Mono.just(Recinto.builder().id(recintoId).capacidadMaxima(50).build()));
        when(zonaPort.buscarPorRecintoId(recintoId)).thenReturn(Flux.empty());
        when(zonaPort.sumarCapacidadesPorRecinto(recintoId)).thenReturn(Mono.just(40));

        StepVerifier.create(useCase.ejecutar(recintoId, Zona.builder().nombre("VIP").capacidad(20).build()))
                .expectError(ZonaCapacidadExcedidaException.class)
                .verify();
    }
}

