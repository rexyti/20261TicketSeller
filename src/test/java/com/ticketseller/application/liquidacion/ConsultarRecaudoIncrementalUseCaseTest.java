package com.ticketseller.application.liquidacion;

import com.ticketseller.domain.exception.EventoNotFoundException;
import com.ticketseller.domain.model.Evento;
import com.ticketseller.domain.model.EstadoEvento;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.LiquidacionQueryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarRecaudoIncrementalUseCaseTest {

    private final EventoRepositoryPort eventoRepositoryPort = mock(EventoRepositoryPort.class);
    private final LiquidacionQueryPort liquidacionQueryPort = mock(LiquidacionQueryPort.class);
    private final ConsultarRecaudoIncrementalUseCase useCase =
            new ConsultarRecaudoIncrementalUseCase(eventoRepositoryPort, liquidacionQueryPort);

    @Test
    void deberiaRetornarRecaudoCuandoEventoExiste() {
        UUID eventoId = UUID.randomUUID();
        Evento evento = Evento.builder().id(eventoId).estado(EstadoEvento.EN_PROGRESO).build();
        Map<String, BigDecimal> recaudo = Map.of(
                "recaudoRegular", BigDecimal.valueOf(1000000),
                "recaudoCortesia", BigDecimal.ZERO,
                "cancelaciones", BigDecimal.valueOf(50000),
                "recaudoNeto", BigDecimal.valueOf(950000)
        );

        when(eventoRepositoryPort.buscarPorId(eventoId)).thenReturn(Mono.just(evento));
        when(liquidacionQueryPort.obtenerRecaudoPorEvento(eventoId)).thenReturn(Mono.just(recaudo));

        StepVerifier.create(useCase.ejecutar(eventoId))
                .assertNext(result -> {
                    assert result.get("recaudoRegular").compareTo(BigDecimal.valueOf(1000000)) == 0;
                    assert result.get("recaudoNeto").compareTo(BigDecimal.valueOf(950000)) == 0;
                    assert result.get("cancelaciones").compareTo(BigDecimal.valueOf(50000)) == 0;
                })
                .verifyComplete();
    }

    @Test
    void deberiaFallarCuandoEventoNoExiste() {
        UUID eventoId = UUID.randomUUID();

        when(eventoRepositoryPort.buscarPorId(eventoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(eventoId))
                .expectError(EventoNotFoundException.class)
                .verify();
    }
}
