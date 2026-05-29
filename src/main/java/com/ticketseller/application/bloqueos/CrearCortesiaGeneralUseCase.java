package com.ticketseller.application.bloqueos;

import com.ticketseller.domain.model.bloqueos.CategoriaCortesia;
import com.ticketseller.domain.model.bloqueos.Cortesia;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class CrearCortesiaGeneralUseCase {

    private final CortesiaTicketCreator cortesiaTicketCreator;

    public Mono<Cortesia> ejecutar(UUID eventoId, UUID destinatarioId, String emailDestinatario,
                                   CategoriaCortesia categoria, UUID zonaId) {
        return cortesiaTicketCreator.crear(eventoId, destinatarioId, emailDestinatario, categoria, zonaId, null);
    }
}
