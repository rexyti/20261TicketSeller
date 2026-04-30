package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.domain.model.zona.PrecioZona;
import com.ticketseller.infrastructure.adapter.in.rest.evento.dto.PrecioZonaRequest;
import com.ticketseller.infrastructure.adapter.in.rest.evento.dto.PrecioZonaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PrecioEventoRestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "eventoId", ignore = true)
    PrecioZona toDomain(PrecioZonaRequest request);

    PrecioZonaResponse toResponse(PrecioZona precioZona);
}

