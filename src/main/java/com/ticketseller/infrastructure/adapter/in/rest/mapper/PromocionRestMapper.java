package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.application.promocion.AplicacionDescuentoResultado;
import com.ticketseller.application.promocion.CrearCodigosPromocionalesCommand;
import com.ticketseller.application.promocion.CrearDescuentoCommand;
import com.ticketseller.application.promocion.CrearPromocionCommand;
import com.ticketseller.domain.model.promocion.CodigoPromocional;
import com.ticketseller.domain.model.promocion.Descuento;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.AplicacionDescuentoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.CodigoPromocionalResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.CrearCodigosRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.CrearDescuentoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.CrearPromocionRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.DescuentoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.promocion.PromocionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface PromocionRestMapper {

    CrearPromocionCommand toCommand(CrearPromocionRequest request);

    @Mapping(target = "promocionId", source = "promocionId")
    @Mapping(target = "acumulable", expression = "java(Boolean.TRUE.equals(request.acumulable()))")
    CrearDescuentoCommand toCommand(UUID promocionId, CrearDescuentoRequest request);

    @Mapping(target = "promocionId", source = "promocionId")
    @Mapping(target = "cantidad", expression = "java(request.cantidad())")
    CrearCodigosPromocionalesCommand toCommand(UUID promocionId, CrearCodigosRequest request);

    PromocionResponse toResponse(Promocion promocion);

    DescuentoResponse toResponse(Descuento descuento);

    CodigoPromocionalResponse toResponse(CodigoPromocional codigoPromocional);

    AplicacionDescuentoResponse toResponse(AplicacionDescuentoResultado resultado);
}

