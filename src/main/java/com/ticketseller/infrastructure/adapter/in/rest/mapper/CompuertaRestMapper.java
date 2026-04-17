package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.domain.model.Compuerta;
import com.ticketseller.infrastructure.adapter.in.rest.dto.compuerta.CompuertaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.compuerta.CrearCompuertaRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CompuertaRestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "recintoId", ignore = true)
    @Mapping(target = "esGeneral", ignore = true)
    Compuerta toDomain(CrearCompuertaRequest request);

    CompuertaResponse toResponse(Compuerta compuerta);
}

