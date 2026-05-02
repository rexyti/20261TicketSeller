package com.ticketseller.infrastructure.adapter.out.persistence.bloqueos.mapper;

import com.ticketseller.domain.model.bloqueos.Cortesia;
import com.ticketseller.infrastructure.adapter.out.persistence.bloqueos.CortesiaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CortesiaPersistenceMapper {
    CortesiaEntity toEntity(Cortesia domain);

    Cortesia toDomain(CortesiaEntity entity);
}
