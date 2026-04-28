package com.ticketseller.application.evento;

import com.ticketseller.application.precios.ConfigurarPreciosUseCase;
import com.ticketseller.domain.exception.evento.EventoNotFoundException;
import com.ticketseller.domain.exception.zona.ZonaSinPrecioException;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.zona.PrecioZona;
import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.PrecioZonaRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigurarPreciosUseCaseTest {

    @Test
    void deberiaFallarSiEventoNoExiste() {
        EventoRepositoryPort eventoRepositoryPort = mock(EventoRepositoryPort.class);
        PrecioZonaRepositoryPort precioZonaRepositoryPort = mock(PrecioZonaRepositoryPort.class);
        ZonaRepositoryPort zonaRepositoryPort = mock(ZonaRepositoryPort.class);
        ConfigurarPreciosUseCase useCase = new ConfigurarPreciosUseCase(eventoRepositoryPort, precioZonaRepositoryPort, zonaRepositoryPort);

        UUID eventoId = UUID.randomUUID();
        when(eventoRepositoryPort.buscarPorId(eventoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(eventoId, List.of()))
                .expectError(EventoNotFoundException.class)
                .verify();
    }

    @Test
    void deberiaFallarSiFaltanZonasConPrecio() {
        EventoRepositoryPort eventoRepositoryPort = mock(EventoRepositoryPort.class);
        PrecioZonaRepositoryPort precioZonaRepositoryPort = mock(PrecioZonaRepositoryPort.class);
        ZonaRepositoryPort zonaRepositoryPort = mock(ZonaRepositoryPort.class);
        ConfigurarPreciosUseCase useCase = new ConfigurarPreciosUseCase(eventoRepositoryPort, precioZonaRepositoryPort, zonaRepositoryPort);

        UUID eventoId = UUID.randomUUID();
        UUID recintoId = UUID.randomUUID();
        UUID zonaA = UUID.randomUUID();
        UUID zonaB = UUID.randomUUID();

        when(eventoRepositoryPort.buscarPorId(eventoId)).thenReturn(Mono.just(Evento.builder().id(eventoId).recintoId(recintoId).build()));
        when(zonaRepositoryPort.buscarPorRecintoId(recintoId)).thenReturn(Flux.just(
                Zona.builder().id(zonaA).build(),
                Zona.builder().id(zonaB).build()
        ));

        List<PrecioZona> request = List.of(PrecioZona.builder().zonaId(zonaA).precio(BigDecimal.TEN).build());

        StepVerifier.create(useCase.ejecutar(eventoId, request))
                .expectError(ZonaSinPrecioException.class)
                .verify();
    }

    @Test
    void deberiaConfigurarPrecios() {
        EventoRepositoryPort eventoRepositoryPort = mock(EventoRepositoryPort.class);
        PrecioZonaRepositoryPort precioZonaRepositoryPort = mock(PrecioZonaRepositoryPort.class);
        ZonaRepositoryPort zonaRepositoryPort = mock(ZonaRepositoryPort.class);
        ConfigurarPreciosUseCase useCase = new ConfigurarPreciosUseCase(eventoRepositoryPort, precioZonaRepositoryPort, zonaRepositoryPort);

        UUID eventoId = UUID.randomUUID();
        UUID recintoId = UUID.randomUUID();
        UUID zonaA = UUID.randomUUID();

        when(eventoRepositoryPort.buscarPorId(eventoId)).thenReturn(Mono.just(Evento.builder().id(eventoId).recintoId(recintoId).build()));
        when(zonaRepositoryPort.buscarPorRecintoId(recintoId)).thenReturn(Flux.just(Zona.builder().id(zonaA).build()));
        when(precioZonaRepositoryPort.eliminarPorEvento(eventoId)).thenReturn(Mono.empty());
        when(precioZonaRepositoryPort.guardar(any(PrecioZona.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        List<PrecioZona> request = List.of(PrecioZona.builder().zonaId(zonaA).precio(BigDecimal.TEN).build());

        StepVerifier.create(useCase.ejecutar(eventoId, request))
                .expectNextMatches(precio -> precio.getEventoId().equals(eventoId))
                .verifyComplete();
    }
}

