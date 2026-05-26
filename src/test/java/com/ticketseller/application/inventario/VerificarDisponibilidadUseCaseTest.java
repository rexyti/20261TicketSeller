package com.ticketseller.application.inventario;

import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.AsientoHold;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.model.asiento.EstadoHold;
import com.ticketseller.domain.repository.AsientoHoldRepositoryPort;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.BloqueoRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VerificarDisponibilidadUseCaseTest {

    private final AsientoRepositoryPort asientoRepositoryPort = mock(AsientoRepositoryPort.class);
    private final AsientoHoldRepositoryPort asientoHoldRepositoryPort = mock(AsientoHoldRepositoryPort.class);
    private final BloqueoRepositoryPort bloqueoRepositoryPort = mock(BloqueoRepositoryPort.class);
    private final VerificarDisponibilidadUseCase useCase = new VerificarDisponibilidadUseCase(
            asientoRepositoryPort, asientoHoldRepositoryPort, bloqueoRepositoryPort);
    private final UUID eventoId = UUID.randomUUID();

    @Test
    void retornaAsientoDisponible() {
        UUID id = UUID.randomUUID();
        Asiento asiento = Asiento.builder().id(id).zonaId(UUID.randomUUID()).estado(EstadoAsiento.DISPONIBLE).build();
        when(asientoRepositoryPort.buscarPorId(id)).thenReturn(Mono.just(asiento));
        when(asientoHoldRepositoryPort.buscarPorAsientoYEvento(id, eventoId)).thenReturn(Flux.empty());
        when(bloqueoRepositoryPort.buscarActivoPorAsientoYEvento(id, eventoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(id, eventoId))
                .assertNext(a -> {
                    assertEquals(AsientoEnEvento.DISPONIBLE, a.estadoEnEvento());
                    assertEquals(id, a.asientoId());
                })
                .verifyComplete();
    }

    @Test
    void retornaAsientoReservadoCuandoHoldActivo() {
        UUID id = UUID.randomUUID();
        Asiento asiento = Asiento.builder().id(id).zonaId(UUID.randomUUID()).estado(EstadoAsiento.DISPONIBLE).build();
        AsientoHold holdActivo = AsientoHold.builder()
                .asientoId(id).eventoId(eventoId).estado(EstadoHold.RESERVADO)
                .expiraEn(LocalDateTime.now().plusMinutes(10)).build();

        when(asientoRepositoryPort.buscarPorId(id)).thenReturn(Mono.just(asiento));
        when(asientoHoldRepositoryPort.buscarPorAsientoYEvento(id, eventoId)).thenReturn(Flux.just(holdActivo));
        when(bloqueoRepositoryPort.buscarActivoPorAsientoYEvento(id, eventoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(id, eventoId))
                .assertNext(a -> assertEquals(AsientoEnEvento.RESERVADO, a.estadoEnEvento()))
                .verifyComplete();
    }

    @Test
    void retornaAsientoVendidoCuandoHoldVendido() {
        UUID id = UUID.randomUUID();
        Asiento asiento = Asiento.builder().id(id).zonaId(UUID.randomUUID()).estado(EstadoAsiento.DISPONIBLE).build();
        AsientoHold holdVendido = AsientoHold.builder()
                .asientoId(id).eventoId(eventoId).estado(EstadoHold.VENDIDO)
                .expiraEn(LocalDateTime.now().plusMinutes(10)).build();

        when(asientoRepositoryPort.buscarPorId(id)).thenReturn(Mono.just(asiento));
        when(asientoHoldRepositoryPort.buscarPorAsientoYEvento(id, eventoId)).thenReturn(Flux.just(holdVendido));
        when(bloqueoRepositoryPort.buscarActivoPorAsientoYEvento(id, eventoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(id, eventoId))
                .assertNext(a -> assertEquals(AsientoEnEvento.VENDIDO, a.estadoEnEvento()))
                .verifyComplete();
    }

    @Test
    void retornaVacioSiAsientoNoExiste() {
        UUID id = UUID.randomUUID();
        when(asientoRepositoryPort.buscarPorId(id)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(id, eventoId))
                .verifyComplete();
    }
}
