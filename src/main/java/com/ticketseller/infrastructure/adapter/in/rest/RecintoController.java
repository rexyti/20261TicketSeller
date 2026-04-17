package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.capacidad.ConfigurarCapacidadUseCase;
import com.ticketseller.application.capacidad.ConfigurarCategoriaUseCase;
import com.ticketseller.application.compuerta.AsignarCompuertaAZonaUseCase;
import com.ticketseller.application.compuerta.CrearCompuertaUseCase;
import com.ticketseller.application.compuerta.ListarCompuertasUseCase;
import com.ticketseller.application.recinto.DesactivarRecintoUseCase;
import com.ticketseller.application.recinto.EditarRecintoUseCase;
import com.ticketseller.application.recinto.ListarRecintosUseCase;
import com.ticketseller.application.recinto.RegistrarRecintoUseCase;
import com.ticketseller.application.zona.CrearZonaUseCase;
import com.ticketseller.application.zona.ListarZonasUseCase;
import com.ticketseller.domain.model.CategoriaRecinto;
import com.ticketseller.domain.model.Compuerta;
import com.ticketseller.domain.model.Recinto;
import com.ticketseller.domain.model.Zona;
import com.ticketseller.infrastructure.adapter.in.rest.dto.recinto.CambiarEstadoRecintoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.compuerta.CompuertaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.recinto.ConfigurarCapacidadRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.recinto.ConfigurarCategoriaRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.compuerta.CrearCompuertaRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.recinto.CrearRecintoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.zona.CrearZonaRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.recinto.EditarRecintoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.recinto.RecintoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.zona.ZonaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.CompuertaRestMapper;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.RecintoRestMapper;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.ZonaRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recintos")
@RequiredArgsConstructor
public class RecintoController {

    private final RegistrarRecintoUseCase registrarRecintoUseCase;
    private final ListarRecintosUseCase listarRecintosUseCase;
    private final ConfigurarCapacidadUseCase configurarCapacidadUseCase;
    private final EditarRecintoUseCase editarRecintoUseCase;
    private final DesactivarRecintoUseCase desactivarRecintoUseCase;
    private final ConfigurarCategoriaUseCase configurarCategoriaUseCase;
    private final RecintoRestMapper recintoRestMapper;

    @PostMapping
    public Mono<ResponseEntity<RecintoResponse>> crear(@Valid @RequestBody CrearRecintoRequest request) {
        return registrarRecintoUseCase.ejecutar(recintoRestMapper.toDomain(request))
                .map(recintoRestMapper::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

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
        Boolean activo = estado == null || "ACTIVO".equalsIgnoreCase(estado);
        return listarRecintosUseCase.ejecutarFiltrado(nombre, categoriaRecinto, ciudad, activo, page, size, sort)
                .map(p -> p.map(recintoRestMapper::toResponse))
                .map(ResponseEntity::ok);
    }

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

    @PatchMapping("/{id}/capacidad")
    public Mono<ResponseEntity<RecintoResponse>> configurarCapacidad(@PathVariable UUID id,
                                                                     @Valid @RequestBody ConfigurarCapacidadRequest request) {
        return configurarCapacidadUseCase.ejecutar(id, request.capacidadMaxima())
                .map(recintoRestMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @PatchMapping("/{id}/categoria")
    public Mono<ResponseEntity<RecintoResponse>> configurarCategoria(@PathVariable UUID id,
                                                                     @Valid @RequestBody ConfigurarCategoriaRequest request) {
        return configurarCategoriaUseCase.ejecutar(id, request.categoria())
                .map(recintoRestMapper::toResponse)
                .map(ResponseEntity::ok);
    }
}
