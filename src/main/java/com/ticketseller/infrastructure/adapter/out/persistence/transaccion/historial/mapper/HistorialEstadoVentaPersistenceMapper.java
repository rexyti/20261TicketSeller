package com.ticketseller.infrastructure.adapter.out.persistence.transaccion.historial.mapper;

import com.ticketseller.domain.model.transaccion.HistorialEstadoVenta;
import com.ticketseller.domain.model.venta.EstadoVenta;
import com.ticketseller.infrastructure.adapter.out.persistence.transaccion.historial.HistorialEstadoVentaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface HistorialEstadoVentaPersistenceMapper {

    @Mapping(target = "estadoAnterior",
            expression = "java(historial.getEstadoAnterior() == null ? null : historial.getEstadoAnterior().name())")
    @Mapping(target = "estadoNuevo",
            expression = "java(historial.getEstadoNuevo() == null ? null : historial.getEstadoNuevo().name())")
    HistorialEstadoVentaEntity toEntity(HistorialEstadoVenta historial);

    @Mapping(target = "estadoAnterior", expression = "java(toEstado(entity.getEstadoAnterior()))")
    @Mapping(target = "estadoNuevo", expression = "java(toEstado(entity.getEstadoNuevo()))")
    HistorialEstadoVenta toDomain(HistorialEstadoVentaEntity entity);

    default EstadoVenta toEstado(String estado) {
        return estado == null ? null : EstadoVenta.valueOf(estado);
    }
}
