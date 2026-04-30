package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.domain.model.conciliacion.Pago;
import com.ticketseller.infrastructure.adapter.in.rest.conciliacion.dto.PagoResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ConciliacionRestMapper {

    PagoResponse toPagoResponse(Pago pago);
}
