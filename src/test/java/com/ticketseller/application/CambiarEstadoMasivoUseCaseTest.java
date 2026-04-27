package com.ticketseller.application;

import com.ticketseller.domain.model.Asiento;
import com.ticketseller.domain.model.EstadoAsiento;
import com.ticketseller.domain.model.HistorialCambioEstado;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.HistorialCambioEstadoRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CambiarEstadoMasivoUseCaseTest {

    private AsientoRepositoryPort asientoRepositoryPort;
    private HistorialCambioEstadoRepositoryPort historialRepositoryPort;
    private CambiarEstadoMasivoUseCase useCase;

    @BeforeEach
    void setUp() {
        asientoRepositoryPort = mock(AsientoRepositoryPort.class);
        historialRepositoryPort = mock(HistorialCambioEstadoRepositoryPort.class);
        useCase = new CambiarEstadoMasivoUseCase(asientoRepositoryPort, historialRepositoryPort);
    }

    @Test
    void todosLosAsientosSeModificanConTransicionValida() {
        UUID eventoId = UUID.randomUUID();
        UUID asientoId1 = UUID.randomUUID();
        UUID asientoId2 = UUID.randomUUID();

        Asiento asiento1 = Asiento.builder().id(asientoId1).estado(EstadoAsiento.DISPONIBLE).build();
        Asiento asiento2 = Asiento.builder().id(asientoId2).estado(EstadoAsiento.DISPONIBLE).build();

        Asiento actualizado1 = asiento1.toBuilder().estado(EstadoAsiento.MANTENIMIENTO).build();
        Asiento actualizado2 = asiento2.toBuilder().estado(EstadoAsiento.MANTENIMIENTO).build();

        when(asientoRepositoryPort.buscarPorId(asientoId1)).thenReturn(Mono.just(asiento1));
        when(asientoRepositoryPort.buscarPorId(asientoId2)).thenReturn(Mono.just(asiento2));
        when(asientoRepositoryPort.guardar(any(Asiento.class)))
                .thenReturn(Mono.just(actualizado1))
                .thenReturn(Mono.just(actualizado2));
        when(historialRepositoryPort.guardar(any(HistorialCambioEstado.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(eventoId, List.of(asientoId1, asientoId2),
                        EstadoAsiento.MANTENIMIENTO, "Mantenimiento general", "admin"))
                .expectNextMatches(response -> response.modificados() == 2 && response.omitidos() == 0)
                .verifyComplete();
    }

    @Test
    void asientoConTransicionInvalidaEsOmitido() {
        UUID eventoId = UUID.randomUUID();
        UUID asientoIdValido = UUID.randomUUID();
        UUID asientoIdInvalido = UUID.randomUUID();

        Asiento asientoValido = Asiento.builder().id(asientoIdValido).estado(EstadoAsiento.DISPONIBLE).build();
        Asiento asientoInvalido = Asiento.builder().id(asientoIdInvalido).estado(EstadoAsiento.VENDIDO).build();

        Asiento actualizado = asientoValido.toBuilder().estado(EstadoAsiento.MANTENIMIENTO).build();

        when(asientoRepositoryPort.buscarPorId(asientoIdValido)).thenReturn(Mono.just(asientoValido));
        when(asientoRepositoryPort.buscarPorId(asientoIdInvalido)).thenReturn(Mono.just(asientoInvalido));
        when(asientoRepositoryPort.guardar(any(Asiento.class))).thenReturn(Mono.just(actualizado));
        when(historialRepositoryPort.guardar(any(HistorialCambioEstado.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(eventoId, List.of(asientoIdValido, asientoIdInvalido),
                        EstadoAsiento.MANTENIMIENTO, "Motivo", "admin"))
                .expectNextMatches(response -> response.modificados() == 1
                        && response.omitidos() >= 1
                        && response.mensajes().stream().anyMatch(m -> m.contains(asientoIdInvalido.toString())))
                .verifyComplete();
    }

    @Test
    void asientoNoEncontradoSeOmiteConMensaje() {
        UUID eventoId = UUID.randomUUID();
        UUID asientoIdExistente = UUID.randomUUID();
        UUID asientoIdNoExistente = UUID.randomUUID();

        Asiento asiento = Asiento.builder().id(asientoIdExistente).estado(EstadoAsiento.DISPONIBLE).build();
        Asiento actualizado = asiento.toBuilder().estado(EstadoAsiento.BLOQUEADO).build();

        when(asientoRepositoryPort.buscarPorId(asientoIdExistente)).thenReturn(Mono.just(asiento));
        when(asientoRepositoryPort.buscarPorId(asientoIdNoExistente)).thenReturn(Mono.empty());
        when(asientoRepositoryPort.guardar(any(Asiento.class))).thenReturn(Mono.just(actualizado));
        when(historialRepositoryPort.guardar(any(HistorialCambioEstado.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(eventoId, List.of(asientoIdExistente, asientoIdNoExistente),
                        EstadoAsiento.BLOQUEADO, "Motivo", "admin"))
                .expectNextMatches(response -> response.modificados() == 1
                        && response.mensajes().stream().anyMatch(m -> m.contains("no encontrado")))
                .verifyComplete();
    }

    @Test
    void listaVaciaRetornaContadoresEnCero() {
        UUID eventoId = UUID.randomUUID();

        StepVerifier.create(useCase.ejecutar(eventoId, List.of(),
                        EstadoAsiento.MANTENIMIENTO, "Motivo", "admin"))
                .expectNextMatches(response -> response.modificados() == 0 && response.omitidos() == 0)
                .verifyComplete();
    }
}
