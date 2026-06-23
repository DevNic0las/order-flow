package com.overflow.order.service.mapper;

import com.overflow.order.domain.Order;
import com.overflow.order.dtos.OrderEventDto;
import com.overflow.order.dtos.OrderResponseDto;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface OrderMapper {


  OrderResponseDto toResponseDto(Order order);

  OrderEventDto toOrderEventDto(Order order);

}
