package com.ticketseller.infrastructure.adapter.out.persistence.mapper;

import com.ticketseller.domain.model.Venta;
import com.ticketseller.infrastructure.adapter.out.persistence.VentaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VentaPersistenceMapper {

    VentaEntity toEntity(Venta venta);

    @Mapping(target = "withEstado", ignore = true)
    Venta toDomain(VentaEntity entity);
}
