package com.ticketseller.application.capacidad;

import com.ticketseller.domain.exception.CapacidadInvalidaException;
import com.ticketseller.domain.exception.recinto.RecintoConEventosException;
import com.ticketseller.domain.exception.recinto.RecintoNotFoundException;
import com.ticketseller.domain.model.recinto.Recinto;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigurarCapacidadUseCaseTest {

    @Test
    void deberiaFallarSiCapacidadEsInvalida() {
        RecintoRepositoryPort repositoryPort = mock(RecintoRepositoryPort.class);
        ConfigurarCapacidadUseCase useCase = new ConfigurarCapacidadUseCase(repositoryPort);

        StepVerifier.create(useCase.ejecutar(UUID.randomUUID(), 0))
                .expectError(CapacidadInvalidaException.class)
                .verify();
    }

    @Test
    void deberiaFallarSiRecintoNoExiste() {
        UUID recintoId = UUID.randomUUID();
        RecintoRepositoryPort repositoryPort = mock(RecintoRepositoryPort.class);
        ConfigurarCapacidadUseCase useCase = new ConfigurarCapacidadUseCase(repositoryPort);

        when(repositoryPort.buscarPorId(recintoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(recintoId, 500))
                .expectError(RecintoNotFoundException.class)
                .verify();
    }

    @Test
    void deberiaFallarSiTieneTicketsVendidosYCambiaCapacidad() {
        UUID recintoId = UUID.randomUUID();
        RecintoRepositoryPort repositoryPort = mock(RecintoRepositoryPort.class);
        ConfigurarCapacidadUseCase useCase = new ConfigurarCapacidadUseCase(repositoryPort);

        Recinto recinto = Recinto.builder()
                .id(recintoId)
                .capacidadMaxima(1000)
                .activo(true)
                .build();

        when(repositoryPort.buscarPorId(recintoId)).thenReturn(Mono.just(recinto));
        when(repositoryPort.tieneTicketsVendidos(recintoId)).thenReturn(Mono.just(true));

        StepVerifier.create(useCase.ejecutar(recintoId, 1200))
                .expectError(RecintoConEventosException.class)
                .verify();
    }

    @Test
    void deberiaActualizarCapacidad() {
        UUID recintoId = UUID.randomUUID();
        RecintoRepositoryPort repositoryPort = mock(RecintoRepositoryPort.class);
        ConfigurarCapacidadUseCase useCase = new ConfigurarCapacidadUseCase(repositoryPort);

        Recinto recinto = Recinto.builder()
                .id(recintoId)
                .nombre("Movistar Arena")
                .ciudad("Bogota")
                .direccion("Calle 1")
                .capacidadMaxima(1000)
                .telefono("3001234567")
                .compuertasIngreso(4)
                .activo(true)
                .build();

        when(repositoryPort.buscarPorId(recintoId)).thenReturn(Mono.just(recinto));
        when(repositoryPort.tieneTicketsVendidos(recintoId)).thenReturn(Mono.just(false));
        when(repositoryPort.guardar(any(Recinto.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(recintoId, 1200))
                .expectNextMatches(updated -> updated.getCapacidadMaxima() == 1200)
                .verifyComplete();
    }
}

