package com.ticketseller.infrastructure.adapter.in.rest.recinto;

import com.ticketseller.application.recinto.ConsultarEstructuraRecintoUseCase;
import com.ticketseller.application.capacidad.ConfigurarCapacidadUseCase;
import com.ticketseller.application.capacidad.ConfigurarCategoriaUseCase;
import com.ticketseller.application.recinto.DesactivarRecintoUseCase;
import com.ticketseller.application.recinto.EditarRecintoUseCase;
import com.ticketseller.application.recinto.ListarRecintosFiltradosUseCase;
import com.ticketseller.application.recinto.RegistrarRecintoUseCase;
import com.ticketseller.domain.model.recinto.CategoriaRecinto;
import com.ticketseller.domain.model.recinto.Recinto;
import com.ticketseller.infrastructure.adapter.in.rest.recinto.dto.CambiarEstadoRecintoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.recinto.dto.ConfigurarCapacidadRequest;
import com.ticketseller.infrastructure.adapter.in.rest.recinto.dto.ConfigurarCategoriaRequest;
import com.ticketseller.infrastructure.adapter.in.rest.recinto.dto.CrearRecintoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.recinto.dto.EditarRecintoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.recinto.dto.RecintoEstructuraResponse;
import com.ticketseller.infrastructure.adapter.in.rest.recinto.dto.RecintoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.RecintoRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Recintos", description = "Gestión de recintos")
@RestController
@RequestMapping("/api/v1/recintos")
@RequiredArgsConstructor
public class RecintoController {

    private final RegistrarRecintoUseCase registrarRecintoUseCase;
    private final ListarRecintosFiltradosUseCase listarRecintosFiltradosUseCase;
    private final ConfigurarCapacidadUseCase configurarCapacidadUseCase;
    private final EditarRecintoUseCase editarRecintoUseCase;
    private final DesactivarRecintoUseCase desactivarRecintoUseCase;
    private final ConfigurarCategoriaUseCase configurarCategoriaUseCase;
    private final ConsultarEstructuraRecintoUseCase consultarEstructuraRecintoUseCase;
    private final RecintoRestMapper recintoRestMapper;

    @Operation(summary = "Registrar un nuevo recinto")
    @PostMapping
    public Mono<ResponseEntity<RecintoResponse>> crear(@Valid @RequestBody CrearRecintoRequest request) {
        return registrarRecintoUseCase.ejecutar(recintoRestMapper.toDomain(request))
                .map(recintoRestMapper::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @Operation(summary = "Listar recintos con filtros y paginación")
    @GetMapping
    public Mono<ResponseEntity<Page<RecintoResponse>>> listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false, defaultValue = "nombre,asc") String sort) {
        CategoriaRecinto categoriaRecinto = categoria == null ? null : CategoriaRecinto.valueOf(categoria);
        Boolean activo = mapearEstado(estado);
        return listarRecintosFiltradosUseCase.ejecutar(nombre, categoriaRecinto, ciudad, activo, page, size, sort)
                .map(resultado -> {
                    var contenido = resultado.contenido().stream().map(recintoRestMapper::toResponse).toList();
                    return new PageImpl<>(contenido,
                            PageRequest.of(resultado.pagina(), resultado.size()),
                            resultado.totalElementos());
                })
                .map(ResponseEntity::ok);
    }

    private Boolean mapearEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return true;
        }
        if ("TODOS".equalsIgnoreCase(estado)) {
            return null;
        }
        return "ACTIVO".equalsIgnoreCase(estado);
    }

    @Operation(summary = "Editar información de un recinto")
    @PatchMapping("/{id}")
    public Mono<ResponseEntity<RecintoResponse>> editar(@PathVariable UUID id,
                                                        @RequestBody EditarRecintoRequest request) {
        Recinto cambios = Recinto.builder()
                .nombre(request.nombre())
                .ciudad(request.ciudad())
                .direccion(request.direccion())
                .capacidadMaxima(request.capacidadMaxima())
                .telefono(request.telefono())
                .compuertasIngreso(request.compuertasIngreso())
                .build();
        return editarRecintoUseCase.ejecutar(id, cambios)
                .map(recintoRestMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Desactivar un recinto")
    @PatchMapping("/{id}/estado")
    public Mono<ResponseEntity<RecintoResponse>> cambiarEstado(@PathVariable UUID id,
                                                               @Valid @RequestBody CambiarEstadoRecintoRequest request) {
        if (Boolean.TRUE.equals(request.activo())) {
            return Mono.error(new IllegalArgumentException("Este endpoint solo permite desactivar recintos"));
        }
        return desactivarRecintoUseCase.ejecutar(id)
                .map(recintoRestMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Configurar capacidad máxima del recinto")
    @PatchMapping("/{id}/capacidad")
    public Mono<ResponseEntity<RecintoResponse>> configurarCapacidad(@PathVariable UUID id,
                                                                     @Valid @RequestBody ConfigurarCapacidadRequest request) {
        return configurarCapacidadUseCase.ejecutar(id, request.capacidadMaxima())
                .map(recintoRestMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Configurar categoría del recinto")
    @PatchMapping("/{id}/categoria")
    public Mono<ResponseEntity<RecintoResponse>> configurarCategoria(@PathVariable UUID id,
                                                                     @Valid @RequestBody ConfigurarCategoriaRequest request) {
        return configurarCategoriaUseCase.ejecutar(id, request.categoria())
                .map(recintoRestMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Consultar estructura del recinto", description = "Retorna la lista de bloques y zonas del recinto para validación de coherencia.")
    @ApiResponse(responseCode = "200", description = "Estructura del recinto recuperada exitosamente",
            content = @Content(schema = @Schema(implementation = RecintoEstructuraResponse.class)))
    @ApiResponse(responseCode = "404", description = "Recinto no encontrado")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<RecintoEstructuraResponse>> consultarEstructura(@PathVariable UUID id) {
        return consultarEstructuraRecintoUseCase.ejecutar(id)
                .map(tuple -> recintoRestMapper.toEstructuraResponse(tuple.getT1(), tuple.getT2()))
                .map(ResponseEntity::ok);
    }
}
