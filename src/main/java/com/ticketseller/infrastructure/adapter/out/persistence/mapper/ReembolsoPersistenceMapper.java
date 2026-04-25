package com.ticketseller.infrastructure.adapter.out.persistence.mapper;

import com.ticketseller.domain.model.Reembolso;
import com.ticketseller.infrastructure.adapter.out.persistence.ReembolsoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReembolsoPersistenceMapper {
    Reembolso toDomain(ReembolsoEntity entity);
    
    @Mapping(target = "id", source = "id")
    ReembolsoEntity toEntity(Reembolso domain);
}
