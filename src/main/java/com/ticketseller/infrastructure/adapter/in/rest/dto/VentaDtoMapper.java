package com.ticketseller.infrastructure.adapter.in.rest.dto;

import com.ticketseller.domain.model.Venta;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface VentaDtoMapper {

    VentaDtoMapper INSTANCE = Mappers.getMapper(VentaDtoMapper.class);

    VentaResponse toResponse(Venta venta);
}
