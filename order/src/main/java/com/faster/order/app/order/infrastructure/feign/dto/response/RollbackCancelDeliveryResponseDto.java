package com.faster.order.app.order.infrastructure.feign.dto.response;

import com.faster.order.app.order.application.dto.response.CancelDeliveryApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.RollbackCancelDeliveryApplicationResponseDto;
import java.util.UUID;

public record RollbackCancelDeliveryResponseDto(
    UUID deliveryId
) {

  public RollbackCancelDeliveryApplicationResponseDto toApplicationDto() {

    return RollbackCancelDeliveryApplicationResponseDto.builder()
        .deliveryId(deliveryId)
        .build();
  }
}
