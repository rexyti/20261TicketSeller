package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.HistorialCambioEstado;
import com.ticketseller.infrastructure.adapter.in.rest.asiento.dto.AsientoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.asiento.dto.DisponibilidadResponse;
import com.ticketseller.infrastructure.adapter.in.rest.asiento.dto.HistorialCambioResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AsientoRestMapper {

    AsientoResponse toAsientoResponse(Asiento asiento);

    HistorialCambioResponse toHistorialResponse(HistorialCambioEstado historial);

    @Mapping(target = "asientoId", source = "id")
    @Mapping(target = "numeroAsiento", source = "numero")
    @Mapping(target = "tipoAsiento", expression = "java(asiento.getTipoAsiento().name())")
    @Mapping(target = "estado", expression = "java(asiento.getEstado().name())")
    DisponibilidadResponse toDisponibilidadResponse(Asiento asiento);
}
