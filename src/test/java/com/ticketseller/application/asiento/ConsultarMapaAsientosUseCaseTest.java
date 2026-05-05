package com.ticketseller.application.asiento;

import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.shared.Pagina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarMapaAsientosUseCaseTest {

    private AsientoRepositoryPort asientoRepositoryPort;
    private ConsultarMapaAsientosUseCase useCase;

    @BeforeEach
    void setUp() {
        asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        useCase = new ConsultarMapaAsientosUseCase(asientoRepositoryPort);
    }

    @Test
    void retornaTodosLosAsientosEnPrimeraPagina() {
        UUID recintoId = UUID.randomUUID();
        UUID zonaId = UUID.randomUUID();

        List<Asiento> asientos = List.of(
                Asiento.builder().id(UUID.randomUUID()).zonaId(zonaId).fila("A").columna(1).estado(EstadoAsiento.DISPONIBLE).build(),
                Asiento.builder().id(UUID.randomUUID()).zonaId(zonaId).fila("A").columna(2).estado(EstadoAsiento.DISPONIBLE).build()
        );

        when(asientoRepositoryPort.buscarPorRecintoId(recintoId)).thenReturn(Flux.fromIterable(asientos));

        StepVerifier.create(useCase.ejecutar(recintoId, 0, 10))
                .expectNextMatches(pagina ->
                        pagina.totalElementos() == 2
                        && pagina.contenido().size() == 2
                        && pagina.pagina() == 0
                        && pagina.size() == 10)
                .verifyComplete();
    }

    @Test
    void paginacionCorrecta() {
        UUID recintoId = UUID.randomUUID();

        List<Asiento> asientos = IntStream.range(0, 5)
                .mapToObj(i -> Asiento.builder()
                        .id(UUID.randomUUID())
                        .fila("A")
                        .columna(i + 1)
                        .estado(EstadoAsiento.DISPONIBLE)
                        .build())
                .toList();

        when(asientoRepositoryPort.buscarPorRecintoId(recintoId)).thenReturn(Flux.fromIterable(asientos));

        StepVerifier.create(useCase.ejecutar(recintoId, 1, 2))
                .expectNextMatches(pagina ->
                        pagina.totalElementos() == 5
                        && pagina.contenido().size() == 2
                        && pagina.pagina() == 1
                        && pagina.totalPaginas() == 3)
                .verifyComplete();
    }

    @Test
    void ultimaPaginaPuedeSerParcial() {
        UUID recintoId = UUID.randomUUID();

        List<Asiento> asientos = IntStream.range(0, 5)
                .mapToObj(i -> Asiento.builder()
                        .id(UUID.randomUUID())
                        .fila("B")
                        .columna(i + 1)
                        .estado(EstadoAsiento.DISPONIBLE)
                        .build())
                .toList();

        when(asientoRepositoryPort.buscarPorRecintoId(recintoId)).thenReturn(Flux.fromIterable(asientos));

        StepVerifier.create(useCase.ejecutar(recintoId, 2, 2))
                .expectNextMatches(pagina ->
                        pagina.totalElementos() == 5
                        && pagina.contenido().size() == 1)
                .verifyComplete();
    }

    @Test
    void recintoSinAsientosDevuelvePaginaVacia() {
        UUID recintoId = UUID.randomUUID();

        when(asientoRepositoryPort.buscarPorRecintoId(recintoId)).thenReturn(Flux.empty());

        StepVerifier.create(useCase.ejecutar(recintoId, 0, 10))
                .expectNextMatches(pagina ->
                        pagina.totalElementos() == 0
                        && pagina.contenido().isEmpty()
                        && pagina.totalPaginas() == 0)
                .verifyComplete();
    }

    @Test
    void paginaFueraDeRangoDevuelveContenidoVacio() {
        UUID recintoId = UUID.randomUUID();

        List<Asiento> asientos = List.of(
                Asiento.builder().id(UUID.randomUUID()).fila("C").columna(1).estado(EstadoAsiento.DISPONIBLE).build()
        );

        when(asientoRepositoryPort.buscarPorRecintoId(recintoId)).thenReturn(Flux.fromIterable(asientos));

        StepVerifier.create(useCase.ejecutar(recintoId, 5, 10))
                .expectNextMatches(pagina ->
                        pagina.totalElementos() == 1
                        && pagina.contenido().isEmpty())
                .verifyComplete();
    }

    @Test
    void retornaPaginaConTipoCorrectoDePagina() {
        UUID recintoId = UUID.randomUUID();

        when(asientoRepositoryPort.buscarPorRecintoId(recintoId)).thenReturn(Flux.fromIterable(
                List.of(Asiento.builder().id(UUID.randomUUID()).fila("A").columna(1).estado(EstadoAsiento.DISPONIBLE).build())
        ));

        StepVerifier.create(useCase.ejecutar(recintoId, 0, 10))
                .expectNextMatches(result -> result instanceof Pagina)
                .verifyComplete();
    }
}
