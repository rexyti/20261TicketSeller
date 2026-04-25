package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.domain.model.Asiento;
import com.ticketseller.infrastructure.adapter.in.rest.dto.tipoasiento.AsientoMapaResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AsientoRestMapper {

    AsientoMapaResponse toResponse(Asiento asiento);
}
