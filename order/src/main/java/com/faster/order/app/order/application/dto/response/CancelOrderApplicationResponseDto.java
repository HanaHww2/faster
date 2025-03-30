package com.faster.order.app.order.application.dto.response;

import com.faster.order.app.order.domain.entity.Order;
import com.faster.order.app.order.domain.enums.OrderStatus;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CancelOrderApplicationResponseDto(
    UUID orderId,
    OrderStatus status
) {

  public static CancelOrderApplicationResponseDto from(Order order) {
    return CancelOrderApplicationResponseDto.builder()
        .orderId(order.getId())
        .status(order.getStatus())
        .build();
  }
}
