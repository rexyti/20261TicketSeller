package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.precios.ConfigurarPreciosUseCase;
import com.ticketseller.application.precios.ListarPreciosUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.dto.evento.ConfigurarPreciosRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.evento.PrecioZonaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.PrecioEventoRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/eventos")
@RequiredArgsConstructor
public class PrecioEventoController {

    private final ConfigurarPreciosUseCase configurarPreciosUseCase;
    private final ListarPreciosUseCase listarPreciosUseCase;
    private final PrecioEventoRestMapper precioEventoRestMapper;

    @PostMapping("/{eventoId}/precios")
    public Flux<PrecioZonaResponse> configurarPrecios(@PathVariable UUID eventoId,
                                                      @Valid @RequestBody ConfigurarPreciosRequest request) {
        var precios = request.precios().stream().map(precioEventoRestMapper::toDomain).toList();
        return configurarPreciosUseCase.ejecutar(eventoId, precios)
                .map(precioEventoRestMapper::toResponse);
    }

    @GetMapping("/{eventoId}/precios")
    public Flux<PrecioZonaResponse> listarPrecios(@PathVariable UUID eventoId) {
        return listarPreciosUseCase.ejecutar(eventoId).map(precioEventoRestMapper::toResponse);
    }
}


