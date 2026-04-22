package com.ticketseller.application.recinto;

import com.ticketseller.domain.exception.RecintoDuplicadoException;
import com.ticketseller.domain.exception.RecintoInvalidoException;
import com.ticketseller.domain.model.Recinto;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrarRecintoUseCaseTest {

    @Test
    void deberiaBloquearDuplicado() {
        RecintoRepositoryPort repositoryPort = mock(RecintoRepositoryPort.class);
        RegistrarRecintoUseCase useCase = new RegistrarRecintoUseCase(repositoryPort);
        Recinto request = Recinto.builder()
                .nombre("Movistar Arena")
                .ciudad("Bogota")
                .direccion("Calle 1")
                .capacidadMaxima(1000)
                .telefono("3001234567")
                .compuertasIngreso(4)
                .build();

        when(repositoryPort.buscarPorNombreYCiudad("Movistar Arena", "Bogota"))
                .thenReturn(Mono.just(Recinto.builder().build()));

        StepVerifier.create(useCase.ejecutar(request))
                .expectError(RecintoDuplicadoException.class)
                .verify();
    }

    @Test
    void deberiaRegistrarRecintoValido() {
        RecintoRepositoryPort repositoryPort = mock(RecintoRepositoryPort.class);
        RegistrarRecintoUseCase useCase = new RegistrarRecintoUseCase(repositoryPort);
        Recinto request = Recinto.builder()
                .nombre("Movistar Arena")
                .ciudad("Bogota")
                .direccion("Calle 1")
                .capacidadMaxima(1000)
                .telefono("3001234567")
                .compuertasIngreso(4)
                .build();

        when(repositoryPort.buscarPorNombreYCiudad("Movistar Arena", "Bogota"))
                .thenReturn(Mono.empty());
        when(repositoryPort.guardar(any(Recinto.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(request))
                .assertNext(saved -> {
                    assert saved.getId() != null;
                    assert saved.getFechaCreacion() != null;
                    assert saved.isActivo();
                    assert "Movistar Arena".equals(saved.getNombre());
                    assert "Bogota".equals(saved.getCiudad());
                })
                .verifyComplete();
    }

    @Test
    void deberiaFallarCuandoDatosDominioNoSonValidos() {
        RecintoRepositoryPort repositoryPort = mock(RecintoRepositoryPort.class);
        RegistrarRecintoUseCase useCase = new RegistrarRecintoUseCase(repositoryPort);
        Recinto request = Recinto.builder()
                .nombre("   ")
                .ciudad("Bogota")
                .direccion("Calle 1")
                .capacidadMaxima(1000)
                .telefono("3001234567")
                .compuertasIngreso(4)
                .build();

        StepVerifier.create(useCase.ejecutar(request))
                .expectError(RecintoInvalidoException.class)
                .verify();

        verify(repositoryPort, never()).buscarPorNombreYCiudad(any(), any());
        verify(repositoryPort, never()).guardar(any());
    }
}

