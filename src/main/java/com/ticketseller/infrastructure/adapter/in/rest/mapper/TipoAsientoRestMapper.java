package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.domain.model.asiento.TipoAsiento;
import com.ticketseller.infrastructure.adapter.in.rest.tipoasiento.dto.TipoAsientoResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TipoAsientoRestMapper {

    @Mapping(target = "estado", expression = "java(tipo.getEstado().name())")
    TipoAsientoResponse toResponse(TipoAsiento tipo);
}
