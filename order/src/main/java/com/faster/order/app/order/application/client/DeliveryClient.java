package com.faster.order.app.order.application.client;

import com.faster.order.app.order.application.dto.request.SaveDeliveryApplicationRequestDto;
import com.faster.order.app.order.application.dto.response.CancelDeliveryApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.RollbackCancelDeliveryApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.SaveDeliveryApplicationResponseDto;
import java.util.UUID;

public interface DeliveryClient {

  SaveDeliveryApplicationResponseDto saveDelivery(
      SaveDeliveryApplicationRequestDto requestDto);

  CancelDeliveryApplicationResponseDto cancelDelivery(UUID deliveryId);

  RollbackCancelDeliveryApplicationResponseDto rollbackCancelDelivery(UUID deliveryId);
}
