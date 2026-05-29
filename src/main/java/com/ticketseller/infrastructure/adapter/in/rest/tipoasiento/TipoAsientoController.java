package com.ticketseller.infrastructure.adapter.in.rest.tipoasiento;

import com.ticketseller.application.tipoasiento.AsignarTipoAsientoAZonaUseCase;
import com.ticketseller.application.tipoasiento.ConsultarTipoAsientoUseCase;
import com.ticketseller.application.tipoasiento.CrearTipoAsientoUseCase;
import com.ticketseller.application.tipoasiento.DesactivarTipoAsientoUseCase;
import com.ticketseller.application.tipoasiento.EditarTipoAsientoUseCase;
import com.ticketseller.application.tipoasiento.ListarTiposAsientoUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.recinto.dto.RecintoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.tipoasiento.dto.AsignarTipoAsientoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.tipoasiento.dto.CrearTipoAsientoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.tipoasiento.dto.EditarTipoAsientoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.tipoasiento.dto.TipoAsientoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.zona.dto.ZonaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.TipoAsientoRestMapper;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.ZonaRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

@Tag(name = "Tipos de Asiento", description = "Gestión de tipos de asiento y asignación a zonas")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TipoAsientoController {

    private final CrearTipoAsientoUseCase crearTipoAsientoUseCase;
    private final ListarTiposAsientoUseCase listarTiposAsientoUseCase;
    private final EditarTipoAsientoUseCase editarTipoAsientoUseCase;
    private final DesactivarTipoAsientoUseCase desactivarTipoAsientoUseCase;
    private final AsignarTipoAsientoAZonaUseCase asignarTipoAsientoAZonaUseCase;
    private final TipoAsientoRestMapper tipoAsientoRestMapper;
    private final ZonaRestMapper zonaRestMapper;
    private final ConsultarTipoAsientoUseCase consultarTipoAsientoUseCase;

    @Operation(summary = "Crear un tipo de asiento")
    @PostMapping("/tipos-asiento")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMINISTRADOR_RECINTO')")
    public Mono<ResponseEntity<TipoAsientoResponse>> crear(@Valid @RequestBody CrearTipoAsientoRequest request) {
        return crearTipoAsientoUseCase.ejecutar(request.nombre(), request.descripcion())
                .map(tipo -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(tipoAsientoRestMapper.toResponse(tipo)));
    }

    @Operation(summary = "Obtener información completa de un Tipo Asiento")
    @ApiResponse(responseCode = "200", description = "Tipo Asiento encontrado")
    @ApiResponse(responseCode = "404", description = "Tipo Asiento no encontrado")
    @GetMapping("/tipos-asiento/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMINISTRADOR_RECINTO', 'GESTOR_INVENTARIO')")
    public Mono<ResponseEntity<TipoAsientoResponse>> obtener(@PathVariable UUID id) {
        return consultarTipoAsientoUseCase.ejecutar(id)
                .map(tipo -> ResponseEntity.ok(tipoAsientoRestMapper.toResponse(tipo)));
    }

    @Operation(summary = "Listar tipos de asiento")
    @GetMapping("/tipos-asiento")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMINISTRADOR_RECINTO', 'GESTOR_INVENTARIO')")
    public Flux<TipoAsientoResponse> listar(@RequestParam(required = false) String estado) {
        return listarTiposAsientoUseCase.ejecutar(estado)
                .map(tipoAsientoRestMapper::toResponse);
    }

    @Operation(summary = "Editar un tipo de asiento")
    @PutMapping("/tipos-asiento/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMINISTRADOR_RECINTO')")
    public Mono<ResponseEntity<TipoAsientoResponse>> editar(@PathVariable UUID id,
                                                            @Valid @RequestBody EditarTipoAsientoRequest request) {
        return editarTipoAsientoUseCase.ejecutar(id, request.nombre(), request.descripcion())
                .map(tipo -> ResponseEntity.ok(tipoAsientoRestMapper.toResponse(tipo)));
    }

    @Operation(summary = "Desactivar un tipo de asiento")
    @DeleteMapping("/tipos-asiento/{id}/desactivar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMINISTRADOR_RECINTO')")
    public Mono<ResponseEntity<TipoAsientoResponse>> desactivar(@PathVariable UUID id) {
        return desactivarTipoAsientoUseCase.ejecutar(id)
                .map(tipo -> ResponseEntity.ok(tipoAsientoRestMapper.toResponse(tipo)));
    }

    @Operation(summary = "Asignar un tipo de asiento a una zona")
    @PostMapping("/recintos/{recintoId}/zonas/{zonaId}/tipo-asiento")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMINISTRADOR_RECINTO')")
    public Mono<ResponseEntity<ZonaResponse>> asignarTipoAsiento(@PathVariable UUID recintoId,
                                                                 @PathVariable UUID zonaId,
                                                                 @Valid @RequestBody AsignarTipoAsientoRequest request) {
        return asignarTipoAsientoAZonaUseCase.ejecutar(recintoId, zonaId, request.tipoAsientoId())
                .map(tuple -> ResponseEntity.ok(zonaRestMapper.toResponse(tuple.getT1())));
    }
}
