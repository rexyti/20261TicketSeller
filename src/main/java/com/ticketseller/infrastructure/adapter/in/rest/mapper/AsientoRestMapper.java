package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.HistorialCambioEstado;
import com.ticketseller.infrastructure.adapter.in.rest.asiento.dto.AsientoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.asiento.dto.HistorialCambioResponse;
import com.ticketseller.infrastructure.adapter.in.rest.tipoasiento.dto.AsientoMapaResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AsientoRestMapper {

    AsientoMapaResponse toResponse(Asiento asiento);

    AsientoResponse toAsientoResponse(Asiento asiento);

    HistorialCambioResponse toHistorialResponse(HistorialCambioEstado historial);
}
