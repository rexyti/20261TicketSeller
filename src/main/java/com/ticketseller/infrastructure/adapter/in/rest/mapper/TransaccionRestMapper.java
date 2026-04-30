package com.ticketseller.infrastructure.adapter.in.rest.mapper;

import com.ticketseller.domain.model.transaccion.HistorialEstadoVenta;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.infrastructure.adapter.in.rest.transaccion.dto.HistorialEstadoVentaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.transaccion.dto.VentaResumenResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransaccionRestMapper {

    VentaResumenResponse toResumen(Venta venta);

    HistorialEstadoVentaResponse toHistorialResponse(HistorialEstadoVenta historial);
}
