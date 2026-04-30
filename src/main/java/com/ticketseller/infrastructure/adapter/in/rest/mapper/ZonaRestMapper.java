package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.domain.model.zona.Zona;
import com.ticketseller.infrastructure.adapter.in.rest.zona.dto.CrearZonaRequest;
import com.ticketseller.infrastructure.adapter.in.rest.zona.dto.ZonaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ZonaRestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "recintoId", ignore = true)
    Zona toDomain(CrearZonaRequest request);

    ZonaResponse toResponse(Zona zona);
}

