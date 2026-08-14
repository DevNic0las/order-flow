package com.orderflow.order.service.mapper;

import com.orderflow.order.domain.Order;
import com.orderflow.order.dtos.OrderEventDto;
import com.orderflow.order.dtos.OrderResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface OrderMapper {


  OrderResponseDto toResponseDto(Order order);

  @Mapping(target = "orderId", source = "id")
  OrderEventDto toOrderEventDto(Order order);

}
