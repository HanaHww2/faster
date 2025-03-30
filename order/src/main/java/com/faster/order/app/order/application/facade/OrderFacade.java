package com.faster.order.app.order.application.facade;

import com.common.resolver.dto.CurrentUserInfoDto;
import com.faster.order.app.order.application.dto.request.SaveOrderApplicationRequestDto;
import com.faster.order.app.order.application.dto.response.CancelOrderApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.InternalConfirmOrderApplicationResponseDto;
import java.util.UUID;

public interface OrderFacade {

  UUID saveOrder(
      CurrentUserInfoDto userInfo, SaveOrderApplicationRequestDto applicationRequestDto);

  InternalConfirmOrderApplicationResponseDto internalConfirmOrderById(UUID orderId);

  CancelOrderApplicationResponseDto cancelOrderById(CurrentUserInfoDto userInfo, UUID orderId);
}
