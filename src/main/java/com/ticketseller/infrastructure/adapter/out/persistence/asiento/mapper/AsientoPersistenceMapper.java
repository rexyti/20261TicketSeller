package com.ticketseller.infrastructure.adapter.out.persistence.asiento.mapper;

import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.infrastructure.adapter.out.persistence.asiento.AsientoEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AsientoPersistenceMapper {
    AsientoEntity toEntity(Asiento domain);
    Asiento toDomain(AsientoEntity entity);
}
