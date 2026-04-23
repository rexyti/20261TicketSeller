package com.ticketseller.infrastructure.adapter.out.persistence.checkout.mapper;

import com.ticketseller.domain.model.EstadoPago;
import com.ticketseller.domain.model.MetodoPago;
import com.ticketseller.domain.model.TransaccionFinanciera;
import com.ticketseller.infrastructure.adapter.out.persistence.checkout.TransaccionFinancieraEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
@SuppressWarnings("unused")
public interface TransaccionFinancieraPersistenceMapper {

    @Mapping(target = "metodoPago", source = "metodoPago", qualifiedByName = "toMetodoPagoString")
    @Mapping(target = "estadoPago", source = "estadoPago", qualifiedByName = "toEstadoPagoString")
    TransaccionFinancieraEntity toEntity(TransaccionFinanciera transaccionFinanciera);

    @Mapping(target = "metodoPago", source = "metodoPago", qualifiedByName = "toMetodoPago")
    @Mapping(target = "estadoPago", source = "estadoPago", qualifiedByName = "toEstadoPago")
    TransaccionFinanciera toDomain(TransaccionFinancieraEntity entity);

    @Named("toMetodoPago")
    default MetodoPago toMetodoPago(String metodoPago) {
        return metodoPago == null ? null : MetodoPago.valueOf(metodoPago);
    }

    @Named("toEstadoPago")
    default EstadoPago toEstadoPago(String estadoPago) {
        return estadoPago == null ? null : EstadoPago.valueOf(estadoPago);
    }

    @Named("toMetodoPagoString")
    default String toMetodoPagoString(MetodoPago metodoPago) {
        return metodoPago == null ? null : metodoPago.name();
    }

    @Named("toEstadoPagoString")
    default String toEstadoPagoString(EstadoPago estadoPago) {
        return estadoPago == null ? null : estadoPago.name();
    }
}

