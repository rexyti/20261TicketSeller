package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.domain.model.Recinto;
import com.ticketseller.infrastructure.adapter.in.rest.dto.recinto.CrearRecintoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.recinto.RecintoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RecintoRestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "activo", constant = "true")
    @Mapping(target = "categoria", ignore = true)
    Recinto toDomain(CrearRecintoRequest request);

    RecintoResponse toResponse(Recinto recinto);
}

