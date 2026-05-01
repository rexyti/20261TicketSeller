package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.domain.model.asiento.HistorialCambioEstado;
import com.ticketseller.infrastructure.adapter.in.rest.asiento.dto.AsientoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.asiento.dto.DisponibilidadResponse;
import com.ticketseller.infrastructure.adapter.in.rest.asiento.dto.HistorialCambioResponse;
import com.ticketseller.infrastructure.adapter.in.rest.tipoasiento.dto.AsientoMapaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = {EstadoAsiento.class})
public interface AsientoRestMapper {

    AsientoMapaResponse toResponse(Asiento asiento);

    AsientoResponse toAsientoResponse(Asiento asiento);

    HistorialCambioResponse toHistorialResponse(HistorialCambioEstado historial);

    @Mapping(target = "asientoId", source = "id")
    @Mapping(target = "disponible", expression = "java(EstadoAsiento.DISPONIBLE.equals(asiento.getEstado()))")
    @Mapping(target = "estado", expression = "java(asiento.getEstado().name())")
    @Mapping(target = "mensaje", expression = "java(EstadoAsiento.DISPONIBLE.equals(asiento.getEstado()) ? null : \"ASIENTO NO DISPONIBLE\")")
    DisponibilidadResponse toDisponibilidadResponse(Asiento asiento);
}
