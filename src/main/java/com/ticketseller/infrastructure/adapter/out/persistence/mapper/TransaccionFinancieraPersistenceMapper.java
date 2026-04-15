package com.ticketseller.infrastructure.adapter.out.persistence.mapper;

import com.ticketseller.domain.model.TransaccionFinanciera;
import com.ticketseller.infrastructure.adapter.out.persistence.TransaccionFinancieraEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransaccionFinancieraPersistenceMapper {

    TransaccionFinancieraEntity toEntity(TransaccionFinanciera transaccion);

    TransaccionFinanciera toDomain(TransaccionFinancieraEntity entity);
}
