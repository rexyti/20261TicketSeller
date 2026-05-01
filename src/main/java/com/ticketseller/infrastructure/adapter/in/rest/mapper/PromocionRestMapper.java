package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.application.promocion.DescuentoAplicado;
import com.ticketseller.application.promocion.ItemCarrito;
import com.ticketseller.domain.model.promocion.Descuento;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.infrastructure.adapter.in.rest.promocion.dto.CalcularDescuentoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.promocion.dto.CrearDescuentoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.promocion.dto.CrearPromocionRequest;
import com.ticketseller.infrastructure.adapter.in.rest.promocion.dto.DescuentoAplicadoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.promocion.dto.DescuentoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.promocion.dto.PromocionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PromocionRestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    Promocion toDomain(CrearPromocionRequest request);

    PromocionResponse toResponse(Promocion promocion);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "promocionId", ignore = true)
    Descuento toDomain(CrearDescuentoRequest request);

    DescuentoResponse toResponse(Descuento descuento);

    DescuentoAplicadoResponse toResponse(DescuentoAplicado descuentoAplicado);

    default List<ItemCarrito> toItems(List<CalcularDescuentoRequest.ItemCarritoDto> dtos) {
        return dtos.stream()
                .map(dto -> new ItemCarrito(dto.zonaId(), dto.precio()))
                .toList();
    }
}
