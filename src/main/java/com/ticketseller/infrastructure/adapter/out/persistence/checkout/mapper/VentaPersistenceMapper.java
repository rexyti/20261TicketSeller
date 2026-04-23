package com.ticketseller.infrastructure.adapter.out.persistence.checkout.mapper;

import com.ticketseller.domain.model.EstadoVenta;
import com.ticketseller.domain.model.Venta;
import com.ticketseller.infrastructure.adapter.out.persistence.checkout.VentaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VentaPersistenceMapper {

    @Mapping(target = "estado", expression = "java(venta.getEstado() == null ? null : venta.getEstado().name())")
    VentaEntity toEntity(Venta venta);

    @Mapping(target = "estado", expression = "java(toEstado(entity.getEstado()))")
    Venta toDomain(VentaEntity entity);

    default EstadoVenta toEstado(String estado) {
        return estado == null ? null : EstadoVenta.valueOf(estado);
    }
}

