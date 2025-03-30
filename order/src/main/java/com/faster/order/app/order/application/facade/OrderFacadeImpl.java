package com.faster.order.app.order.application.facade;

import com.common.exception.CustomException;
import com.common.resolver.dto.CurrentUserInfoDto;
import com.faster.order.app.order.application.dto.request.SaveOrderApplicationRequestDto;
import com.faster.order.app.order.application.dto.response.CancelOrderApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.InternalConfirmOrderApplicationResponseDto;
import com.faster.order.app.order.application.usecase.OrderService;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderFacadeImpl implements OrderFacade {
  private final OrderService orderService;

  @Override
  public UUID saveOrder(
      CurrentUserInfoDto userInfo, SaveOrderApplicationRequestDto applicationRequestDto) {

    UUID orderId = null;
    try {
      orderId = orderService.saveOrder(userInfo, applicationRequestDto);
    } catch (CustomException e) { // 커스텀 익셉션의 경우에는 현재 로직상 롤백 수행이 필요없음

      throw e;
    } catch (RuntimeException e) { // 그 외 예외 발생시,

      Map<UUID, Integer> productStocksMap = applicationRequestDto.toProductStocksMapForRollback();
      orderService.updateStocks(productStocksMap);
      throw e;
    } // 트랜잭션 롤백이 안되는 기타 예외 경우에는 - 에러 정보 추가 확인 필요

    return orderId;
  }

  @Transactional
  @Override
  public InternalConfirmOrderApplicationResponseDto internalConfirmOrderById(UUID orderId) {


    InternalConfirmOrderApplicationResponseDto confirmResponseDto = null;
    try {
      confirmResponseDto = orderService.internalConfirmOrderById(orderId);
    } catch (CustomException e) { // 커스텀 익셉션의 경우에는 현재 로직상 추가 작업 수행이 필요없음
      throw e;
    } catch (RuntimeException e) { // 그 외 런타임 예외 발생시,
      // 배송 접수 취소
      orderService.cancelDeliveryByOrderId(orderId);
      throw e;
    } // 트랜잭션 롤백이 안되는 기타 예외 경우에는 - 에러 정보 추가 확인 필요

    return confirmResponseDto;
  }

  @Transactional
  @Override
  public CancelOrderApplicationResponseDto cancelOrderById(CurrentUserInfoDto userInfo,
      UUID orderId) {

    CancelOrderApplicationResponseDto cancelResponseDto = null;
    try {
      cancelResponseDto = orderService.cancelOrderById(userInfo, orderId);
    } catch (CustomException e) { // 커스텀 익셉션의 경우에는 현재 로직상 추가 작업 수행이 필요없음
      throw e;
    } catch (RuntimeException e) { // 그 외 예외 발생시,

      // 복구된 재고 재차감
      orderService.rollbackUpdateStocksByOrderId(orderId);
      // 배송 취소 복구
      orderService.rollbackCancelDeliveryByOrderId(orderId);
      throw e;
    } // 트랜잭션 롤백이 안되는 기타 예외 경우에는 - 에러 정보 추가 확인 필요

    return cancelResponseDto;
  }
}
