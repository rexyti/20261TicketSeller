package com.ticketseller.infrastructure.adapter.in.rest.evento;

import com.ticketseller.application.precios.ConfigurarPreciosUseCase;
import com.ticketseller.application.precios.ListarPreciosUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.evento.dto.ConfigurarPreciosRequest;
import com.ticketseller.infrastructure.adapter.in.rest.evento.dto.PrecioZonaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.PrecioEventoRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Precios de Evento", description = "Configuración de precios por zona para un evento")
@RestController
@RequestMapping("/api/v1/eventos")
@RequiredArgsConstructor
public class PrecioEventoController {

    private final ConfigurarPreciosUseCase configurarPreciosUseCase;
    private final ListarPreciosUseCase listarPreciosUseCase;
    private final PrecioEventoRestMapper precioEventoRestMapper;

    @Operation(summary = "Configurar precios de zonas para un evento")
    @PostMapping("/{eventoId}/precios")
    public Flux<PrecioZonaResponse> configurarPrecios(@PathVariable UUID eventoId,
                                                      @Valid @RequestBody ConfigurarPreciosRequest request) {
        var precios = request.precios().stream().map(precioEventoRestMapper::toDomain).toList();
        return configurarPreciosUseCase.ejecutar(eventoId, precios)
                .map(precioEventoRestMapper::toResponse);
    }

    @Operation(summary = "Listar precios configurados para un evento")
    @GetMapping("/{eventoId}/precios")
    public Flux<PrecioZonaResponse> listarPrecios(@PathVariable UUID eventoId) {
        return listarPreciosUseCase.ejecutar(eventoId).map(precioEventoRestMapper::toResponse);
    }
}


