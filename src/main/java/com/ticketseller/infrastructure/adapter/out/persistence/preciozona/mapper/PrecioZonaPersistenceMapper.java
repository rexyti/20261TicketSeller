package com.ticketseller.infrastructure.adapter.out.persistence.preciozona.mapper;

import com.ticketseller.domain.model.zona.PrecioZona;
import com.ticketseller.infrastructure.adapter.out.persistence.preciozona.PrecioZonaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PrecioZonaPersistenceMapper {

    PrecioZonaEntity toEntity(PrecioZona precioZona);

    PrecioZona toDomain(PrecioZonaEntity entity);
}

