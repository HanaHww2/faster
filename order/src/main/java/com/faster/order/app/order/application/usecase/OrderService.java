package com.faster.order.app.order.application.usecase;

import com.common.resolver.dto.CurrentUserInfoDto;
import com.common.response.PageResponse;
import com.faster.order.app.order.application.dto.request.SaveOrderApplicationRequestDto;
import com.faster.order.app.order.application.dto.request.SearchOrderConditionDto;
import com.faster.order.app.order.application.dto.response.CancelDeliveryApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.CancelOrderApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.GetOrderDetailApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.IGetOrderDetailApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.InternalConfirmOrderApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.InternalUpdateOrderStatusApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.SaveDeliveryApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.SearchOrderApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.UpdateStocksApplicationResponseDto;
import com.faster.order.app.order.domain.entity.Order;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface OrderService {

  PageResponse<SearchOrderApplicationResponseDto> getOrdersByCondition(CurrentUserInfoDto userInfo,
      Pageable pageable, SearchOrderConditionDto condition);

  GetOrderDetailApplicationResponseDto getOrderById(CurrentUserInfoDto userInfo, UUID orderId);

  CancelOrderApplicationResponseDto cancelOrderById(CurrentUserInfoDto userInfo, UUID orderId);

  void deleteOrderById(CurrentUserInfoDto userInfo, UUID orderId);

  UpdateStocksApplicationResponseDto decreaseStocks(Map<UUID, Integer> productStockMap);

  UpdateStocksApplicationResponseDto increaseStocks(Map<UUID, Integer> productStockMap);

  CancelDeliveryApplicationResponseDto cancelDelivery(UUID deliveryId);

  InternalConfirmOrderApplicationResponseDto internalConfirmOrderById(UUID orderId);

  InternalUpdateOrderStatusApplicationResponseDto internalUpdateOrderStatusById(UUID orderId, String status);

  UUID saveOrder(CurrentUserInfoDto userInfo, SaveOrderApplicationRequestDto applicationRequestDto);

  IGetOrderDetailApplicationResponseDto internalGetOrderById(UUID orderId);

  UpdateStocksApplicationResponseDto updateStocks(Map<UUID, Integer> productStockMap);

  SaveDeliveryApplicationResponseDto saveDelivery(Order order);

  CancelDeliveryApplicationResponseDto cancelDeliveryByOrderId(UUID orderId);

  void rollbackCancelDelivery(UUID deliveryId);

  void rollbackUpdateStocksByOrderId(UUID orderId);

  void rollbackCancelDeliveryByOrderId(UUID orderId);
}
