package com.ticketseller.application.recinto;

import com.ticketseller.domain.model.CategoriaRecinto;
import com.ticketseller.domain.model.Recinto;
import com.ticketseller.domain.shared.Pagina;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListarRecintosFiltradosUseCaseTest {

    @Test
    void deberiaDelegarConsultaFiltradaAlRepositorio() {
        RecintoRepositoryPort repositoryPort = mock(RecintoRepositoryPort.class);
        ListarRecintosFiltradosUseCase useCase = new ListarRecintosFiltradosUseCase(repositoryPort);
        Pagina<Recinto> esperado = new Pagina<>(
                List.of(Recinto.builder().nombre("Movistar Arena").build()),
                1,
                0,
                10
        );

        when(repositoryPort.listarFiltrados("Movi", CategoriaRecinto.TEATRO, "Bogota", true, 0, 10, "nombre,asc"))
                .thenReturn(Mono.just(esperado));

        StepVerifier.create(useCase.ejecutar("Movi", CategoriaRecinto.TEATRO, "Bogota", true, 0, 10, "nombre,asc"))
                .expectNext(esperado)
                .verifyComplete();

        verify(repositoryPort).listarFiltrados("Movi", CategoriaRecinto.TEATRO, "Bogota", true, 0, 10, "nombre,asc");
    }
}


