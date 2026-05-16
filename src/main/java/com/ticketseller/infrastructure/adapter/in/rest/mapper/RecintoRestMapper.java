package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.domain.model.recinto.Recinto;
import com.ticketseller.domain.model.zona.Compuerta;
import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.infrastructure.adapter.in.rest.recinto.dto.CrearRecintoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.recinto.dto.RecintoEstructuraResponse;
import com.ticketseller.infrastructure.adapter.in.rest.recinto.dto.RecintoEstructuraResponse.ZonaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.recinto.dto.RecintoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface RecintoRestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "activo", constant = "true")
    @Mapping(target = "categoria", ignore = true)
    Recinto toDomain(CrearRecintoRequest request);

    RecintoResponse toResponse(Recinto recinto);

    default RecintoEstructuraResponse toEstructuraResponse(Recinto recinto, List<Zona> zonas, List<Compuerta> compuertas) {
        var zonasResponse = zonas.stream()
                .map(zona -> {
                    var nombresCompuertas = compuertas.stream()
                            .filter(c -> zona.getId().equals(c.getZonaId()))
                            .map(Compuerta::getNombre)
                            .collect(Collectors.toList());
                    return new ZonaResponse(zona.getNombre(), nombresCompuertas);
                })
                .collect(Collectors.toList());
        return new RecintoEstructuraResponse(recinto.getId(), zonasResponse);
    }
}

