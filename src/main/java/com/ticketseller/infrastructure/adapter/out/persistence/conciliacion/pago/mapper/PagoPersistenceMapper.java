package com.ticketseller.infrastructure.adapter.out.persistence.conciliacion.pago.mapper;

import com.ticketseller.domain.model.conciliacion.EstadoConciliacion;
import com.ticketseller.domain.model.conciliacion.Pago;
import com.ticketseller.infrastructure.adapter.out.persistence.conciliacion.pago.PagoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PagoPersistenceMapper {

    @Mapping(target = "estado",
            expression = "java(pago.getEstado() == null ? null : pago.getEstado().name())")
    PagoEntity toEntity(Pago pago);

    @Mapping(target = "estado", expression = "java(toEstado(entity.getEstado()))")
    Pago toDomain(PagoEntity entity);

    default EstadoConciliacion toEstado(String estado) {
        return estado == null ? null : EstadoConciliacion.valueOf(estado);
    }
}
