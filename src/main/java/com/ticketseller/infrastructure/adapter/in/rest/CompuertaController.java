package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.compuerta.AsignarCompuertaAZonaUseCase;
import com.ticketseller.application.compuerta.CrearCompuertaUseCase;
import com.ticketseller.application.compuerta.ListarCompuertasUseCase;
import com.ticketseller.domain.model.zona.Compuerta;
import com.ticketseller.infrastructure.adapter.in.rest.dto.compuerta.CompuertaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.compuerta.CrearCompuertaRequest;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.CompuertaRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Compuertas", description = "Gestión de compuertas de entrada de un recinto")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recintos")
public class CompuertaController {
    private final CrearCompuertaUseCase crearCompuertaUseCase;
    private final AsignarCompuertaAZonaUseCase asignarCompuertaAZonaUseCase;
    private final ListarCompuertasUseCase listarCompuertasUseCase;
    private final CompuertaRestMapper compuertaRestMapper;

    @Operation(summary = "Crear una compuerta en un recinto")
    @PostMapping("/{recintoId}/compuertas")
    public Mono<ResponseEntity<CompuertaResponse>> crearCompuerta(@PathVariable UUID recintoId,
                                                                  @Valid @RequestBody CrearCompuertaRequest request) {
        Compuerta compuerta = compuertaRestMapper.toDomain(request);
        return crearCompuertaUseCase.ejecutar(recintoId, compuerta)
                .map(compuertaRestMapper::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @Operation(summary = "Listar compuertas de un recinto")
    @GetMapping("/{recintoId}/compuertas")
    public Flux<CompuertaResponse> listarCompuertas(@PathVariable UUID recintoId) {
        return listarCompuertasUseCase.ejecutar(recintoId).map(compuertaRestMapper::toResponse);
    }

    @Operation(summary = "Asignar compuerta a una zona")
    @PatchMapping("/zonas/{zonaId}/compuertas/{compuertaId}")
    public Mono<ResponseEntity<CompuertaResponse>> asignarCompuertaZona(@PathVariable UUID compuertaId,
                                                                        @PathVariable UUID zonaId) {
        return asignarCompuertaAZonaUseCase.ejecutar(compuertaId, zonaId)
                .map(compuertaRestMapper::toResponse)
                .map(ResponseEntity::ok);
    }
}
