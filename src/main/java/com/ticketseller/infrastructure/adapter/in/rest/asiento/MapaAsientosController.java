package com.ticketseller.infrastructure.adapter.in.rest.asiento;

import com.ticketseller.application.asiento.AsignarAsientosAZonaUseCase;
import com.ticketseller.application.asiento.ConsultarMapaAsientosUseCase;
import com.ticketseller.application.asiento.CrearMapaAsientosUseCase;
import com.ticketseller.application.asiento.MarcarEspacioVacioUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.tipoasiento.dto.AsientoMapaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.asiento.dto.AsignarAsientosAZonaRequest;
import com.ticketseller.infrastructure.adapter.in.rest.asiento.dto.CrearMapaAsientosRequest;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.AsientoRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Tag(name = "Mapa de Asientos", description = "Gestión del mapa de asientos de un recinto")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recintos")
public class MapaAsientosController {

    private final CrearMapaAsientosUseCase crearMapaAsientosUseCase;
    private final MarcarEspacioVacioUseCase marcarEspacioVacioUseCase;
    private final AsignarAsientosAZonaUseCase asignarAsientosAZonaUseCase;
    private final ConsultarMapaAsientosUseCase consultarMapaAsientosUseCase;
    private final AsientoRestMapper asientoRestMapper;

    @Operation(summary = "Crear mapa de asientos NxM para un recinto")
    @PostMapping("/{recintoId}/mapa")
    public Flux<AsientoMapaResponse> crearMapa(@PathVariable UUID recintoId,
                                               @Valid @RequestBody CrearMapaAsientosRequest request) {
        return crearMapaAsientosUseCase.ejecutar(recintoId, request.filas(), request.columnasPorFila())
                .map(asientoRestMapper::toResponse);
    }

    @Operation(summary = "Marcar un asiento como espacio vacío")
    @PatchMapping("/{recintoId}/mapa/asientos/{asientoId}")
    public Mono<ResponseEntity<AsientoMapaResponse>> marcarEspacioVacio(@PathVariable UUID recintoId,
                                                                        @PathVariable UUID asientoId) {
        return marcarEspacioVacioUseCase.ejecutar(asientoId)
                .map(asientoRestMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Asignar una lista de asientos a una zona")
    @PostMapping("/{recintoId}/zonas/{zonaId}/asientos")
    public Flux<AsientoMapaResponse> asignarAsientosAZona(@PathVariable UUID recintoId,
                                                          @PathVariable UUID zonaId,
                                                          @Valid @RequestBody AsignarAsientosAZonaRequest request) {
        return asignarAsientosAZonaUseCase.ejecutar(request.asientoIds(), zonaId)
                .map(asientoRestMapper::toResponse);
    }

    @Operation(summary = "Consultar mapa de asientos del recinto paginado")
    @GetMapping("/{recintoId}/mapa/asientos")
    public Mono<Page<AsientoMapaResponse>> consultarMapa(@PathVariable UUID recintoId,
                                                         @RequestParam(defaultValue = "0") int pagina,
                                                         @RequestParam(defaultValue = "20") int size) {
        return consultarMapaAsientosUseCase.ejecutar(recintoId, pagina, size)
                .map(p -> {
                    List<AsientoMapaResponse> responses = p.contenido().stream()
                            .map(asientoRestMapper::toResponse)
                            .toList();
                    return new PageImpl<>(responses, PageRequest.of(p.pagina(), p.size()), p.totalElementos());
                });
    }
}
