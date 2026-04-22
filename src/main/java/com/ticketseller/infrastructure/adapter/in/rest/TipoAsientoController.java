package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.tipoasiento.AsignarTipoAsientoAZonaUseCase;
import com.ticketseller.application.tipoasiento.CrearTipoAsientoUseCase;
import com.ticketseller.application.tipoasiento.DesactivarTipoAsientoUseCase;
import com.ticketseller.application.tipoasiento.EditarTipoAsientoUseCase;
import com.ticketseller.application.tipoasiento.ListarTiposAsientoUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.dto.tipoasiento.AsignarTipoAsientoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.tipoasiento.CambiarEstadoTipoAsientoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.tipoasiento.CrearTipoAsientoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.tipoasiento.EditarTipoAsientoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.tipoasiento.TipoAsientoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.zona.ZonaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.ZonaRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TipoAsientoController {

    private final CrearTipoAsientoUseCase crearTipoAsientoUseCase;
    private final ListarTiposAsientoUseCase listarTiposAsientoUseCase;
    private final EditarTipoAsientoUseCase editarTipoAsientoUseCase;
    private final DesactivarTipoAsientoUseCase desactivarTipoAsientoUseCase;
    private final AsignarTipoAsientoAZonaUseCase asignarTipoAsientoAZonaUseCase;
    private final ZonaRestMapper zonaRestMapper;

    @PostMapping("/tipos-asiento")
    public Mono<ResponseEntity<TipoAsientoResponse>> crear(@Valid @RequestBody CrearTipoAsientoRequest request) {
        return crearTipoAsientoUseCase.ejecutar(request.nombre(), request.descripcion())
                .map(tuple -> {
                    var tipo = tuple.getT1();
                    String advertencia = tuple.getT2().isEmpty() ? null : tuple.getT2();
                    TipoAsientoResponse response = new TipoAsientoResponse(
                            tipo.getId(), tipo.getNombre(), tipo.getDescripcion(),
                            tipo.getEstado().name(), false, advertencia);
                    return ResponseEntity.status(HttpStatus.CREATED).body(response);
                });
    }

    @GetMapping("/tipos-asiento")
    public Flux<TipoAsientoResponse> listar(@RequestParam(required = false) String estado) {
        return listarTiposAsientoUseCase.ejecutar(estado)
                .flatMap(tipo -> listarTiposAsientoUseCase.calcularEnUso(tipo)
                        .next()
                        .defaultIfEmpty(false)
                        .map(enUso -> new TipoAsientoResponse(
                                tipo.getId(), tipo.getNombre(), tipo.getDescripcion(),
                                tipo.getEstado().name(), enUso, null)));
    }

    @PutMapping("/tipos-asiento/{id}")
    public Mono<ResponseEntity<TipoAsientoResponse>> editar(@PathVariable UUID id,
                                                             @Valid @RequestBody EditarTipoAsientoRequest request) {
        return editarTipoAsientoUseCase.ejecutar(id, request.nombre(), request.descripcion())
                .map(tipo -> {
                    TipoAsientoResponse response = new TipoAsientoResponse(
                            tipo.getId(), tipo.getNombre(), tipo.getDescripcion(),
                            tipo.getEstado().name(), false, null);
                    return ResponseEntity.ok(response);
                });
    }

    @PatchMapping("/tipos-asiento/{id}/estado")
    public Mono<ResponseEntity<TipoAsientoResponse>> cambiarEstado(@PathVariable UUID id,
                                                                    @Valid @RequestBody CambiarEstadoTipoAsientoRequest request) {
        return desactivarTipoAsientoUseCase.ejecutar(id)
                .map(tipo -> {
                    TipoAsientoResponse response = new TipoAsientoResponse(
                            tipo.getId(), tipo.getNombre(), tipo.getDescripcion(),
                            tipo.getEstado().name(), false, null);
                    return ResponseEntity.ok(response);
                });
    }

    @PostMapping("/recintos/{recintoId}/zonas/{zonaId}/tipo-asiento")
    public Mono<ResponseEntity<ZonaResponse>> asignarTipoAsiento(@PathVariable UUID recintoId,
                                                                  @PathVariable UUID zonaId,
                                                                  @Valid @RequestBody AsignarTipoAsientoRequest request) {
        return asignarTipoAsientoAZonaUseCase.ejecutar(recintoId, zonaId, request.tipoAsientoId())
                .map(tuple -> {
                    var zona = tuple.getT1();
                    ZonaResponse response = zonaRestMapper.toResponse(zona);
                    return ResponseEntity.ok(response);
                });
    }
}
