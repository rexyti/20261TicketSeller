package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.model.bloqueos.CategoriaCortesia;
import com.ticketseller.domain.model.bloqueos.Cortesia;
import com.ticketseller.domain.model.bloqueos.EstadoCortesia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrearCortesiaGeneralUseCaseTest {

    private CortesiaTicketCreator cortesiaTicketCreator;
    private CrearCortesiaGeneralUseCase useCase;

    private final UUID eventoId = UUID.randomUUID();
    private final UUID zonaId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cortesiaTicketCreator = mock(CortesiaTicketCreator.class);
        useCase = new CrearCortesiaGeneralUseCase(cortesiaTicketCreator);
    }

    @Test
    void delegaCreacionAlCreator() {
        Cortesia cortesia = Cortesia.builder()
                .id(UUID.randomUUID())
                .eventoId(eventoId)
                .destinatario("Prensa ABC")
                .categoria(CategoriaCortesia.PRENSA)
                .ticketId(UUID.randomUUID())
                .estado(EstadoCortesia.GENERADA)
                .build();

        when(cortesiaTicketCreator.crear(eventoId, "Prensa ABC", CategoriaCortesia.PRENSA, zonaId, null))
                .thenReturn(Mono.just(cortesia));

        StepVerifier.create(useCase.ejecutar(eventoId, "Prensa ABC", CategoriaCortesia.PRENSA, zonaId))
                .expectNext(cortesia)
                .verifyComplete();

        verify(cortesiaTicketCreator).crear(eventoId, "Prensa ABC", CategoriaCortesia.PRENSA, zonaId, null);
    }
}
