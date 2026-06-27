package com.overflow.order.service.mapper;

import com.overflow.order.domain.Order;
import com.overflow.order.dtos.OrderEventDto;
import com.overflow.order.dtos.OrderResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface OrderMapper {


  OrderResponseDto toResponseDto(Order order);

  @Mapping(target = "orderId", source = "id")
  OrderEventDto toOrderEventDto(Order order);

}
