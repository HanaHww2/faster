package com.faster.order.app.order.application.usecase;

import com.common.exception.CustomException;
import com.common.resolver.dto.CurrentUserInfoDto;
import com.common.resolver.dto.UserRole;
import com.common.response.PageResponse;
import com.faster.order.app.global.exception.OrderErrorCode;
import com.faster.order.app.order.application.client.CompanyClient;
import com.faster.order.app.order.application.client.DeliveryClient;
import com.faster.order.app.order.application.client.ProductClient;
import com.faster.order.app.order.application.dto.response.GetProductsApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.GetProductsApplicationResponseDto.GetProductApplicationResponseDto;
import com.faster.order.app.order.application.dto.request.SaveDeliveryApplicationRequestDto;
import com.faster.order.app.order.application.dto.request.SaveOrderApplicationRequestDto;
import com.faster.order.app.order.application.dto.request.SaveOrderApplicationRequestDto.SaveOrderItemApplicationRequestDto;
import com.faster.order.app.order.application.dto.request.SearchOrderConditionDto;
import com.faster.order.app.order.application.dto.request.UpdateStocksApplicationRequestDto;
import com.faster.order.app.order.application.dto.response.CancelDeliveryApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.CancelOrderApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.GetCompanyApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.GetOrderDetailApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.IGetOrderDetailApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.InternalConfirmOrderApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.InternalUpdateOrderStatusApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.RollbackCancelDeliveryApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.SaveDeliveryApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.SearchOrderApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.UpdateStocksApplicationResponseDto;
import com.faster.order.app.order.domain.entity.Order;
import com.faster.order.app.order.domain.enums.OrderStatus;
import com.faster.order.app.order.domain.repository.OrderRepository;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderServiceImpl implements OrderService {

  private final OrderRepository orderRepository;
  private final ProductClient productClient;
  private final CompanyClient companyClient;
  private final DeliveryClient deliveryClient;

  @Transactional(readOnly = true)
  @Override
  public PageResponse<SearchOrderApplicationResponseDto> getOrdersByCondition(CurrentUserInfoDto userInfo,
      Pageable pageable, SearchOrderConditionDto condition) {

    // 1. 마스터 - 모든 주문 조회 가능
    // 2. 업체 담당자 - 해당 업체 주문만 조회 가능
    GetCompanyApplicationResponseDto company = getCompanyDtoByUser(userInfo);
    UUID companyId = company == null ? null : company.id();

    Page<SearchOrderApplicationResponseDto> pageList = orderRepository.getOrdersByConditionAndCompanyId(
            pageable, condition.toCriteria(), companyId, userInfo.role())
        .map(SearchOrderApplicationResponseDto::from);
    return PageResponse.from(pageList);
  }

  @Transactional(readOnly = true)
  @Override
  public GetOrderDetailApplicationResponseDto getOrderById(CurrentUserInfoDto userInfo, UUID orderId) {

    Order order = orderRepository.findByIdAndDeletedAtIsNullFetchJoin(orderId)
        .orElseThrow(() -> new CustomException(OrderErrorCode.INVALID_ORDER_ID));

    // 권한 검증
    // 1. 마스터 - 모든 주문 조회 가능
    // 2. 업체 담당자 - 해당 업체 주문 조회 가능
    this.checkIfValidAccessToModify(userInfo, order.getReceivingCompanyId(), OrderErrorCode.FORBIDDEN);

    return GetOrderDetailApplicationResponseDto.from(order);
  }

  //@Transactional
  @Override
  public UUID saveOrder(CurrentUserInfoDto userInfo,
      SaveOrderApplicationRequestDto applicationRequestDto) {

    // 권한 검증
    // 1. 마스터 - 모든 주문 생성 가능
    // 2. 업체 담당자 - 해당 업체 주문 생성 가능
    this.checkIfValidAccessToModify(
        userInfo, applicationRequestDto.receivingCompanyId(), OrderErrorCode.FORBIDDEN_SAVE);

    Map<UUID, Integer> productStocksMap = applicationRequestDto.toProductStocksMap();

    // 상품 정보 조회하여 검증 로직 수행
    this.validateOrderRequest(applicationRequestDto, productStocksMap);
    Order order = applicationRequestDto.toEntity();

    // 재고 차감 요청 수행
    UpdateStocksApplicationResponseDto updateStocksDto = this.decreaseStocks(productStocksMap);

    orderRepository.save(order);
    return order.getId();
  }

  @Transactional
  @Override
  public CancelOrderApplicationResponseDto cancelOrderById(CurrentUserInfoDto userInfo, UUID orderId) {

    Order order = orderRepository.findByIdAndStatusAndDeletedAtIsNullFetchJoin(orderId, OrderStatus.CONFIRMED)
        .orElseThrow(() -> new CustomException(OrderErrorCode.UNABLE_CANCEL));

    // 권한 검증
    // 1. 마스터 - 모든 주문 취소 가능
    // 2. 업체 담당자 - 해당 업체 주문 취소 가능
    this.checkIfValidAccessToModify(userInfo, order.getReceivingCompanyId(), OrderErrorCode.FORBIDDEN);

    // 1. 배송 취소 처리
    CancelDeliveryApplicationResponseDto responseDto = this.cancelDelivery(order.getDeliveryId());

    // 2. 결제 취소 처리 - 생략

    // 3. 결제 취소 후 재고 상품 롤백
    UpdateStocksApplicationResponseDto updateStocksApplicationResponseDto = null;
    try {
      updateStocksApplicationResponseDto = productClient.increaseStocks(
          UpdateStocksApplicationRequestDto.from(order.getOrderItems()));
    } catch (CustomException e) {
      this.rollbackCancelDelivery(order.getDeliveryId());
      throw new CustomException(OrderErrorCode.FAIL_CANCEL_ORDER);
    }

    // 4. 주문 취소
    order.cancel();
    return CancelOrderApplicationResponseDto.from(order);
  }

  @Transactional
  @Override
  public void deleteOrderById(CurrentUserInfoDto userInfo, UUID orderId) {

    // 삭제는 완료 혹은 취소 상태인 주문에만 가능
    Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
        .orElseThrow(() -> new CustomException(OrderErrorCode.INVALID_ORDER_ID));

    // 권한 검증
    // 1. 마스터 - 모든 업체 주문 삭제 가능
    // 2. 업체 담당자 - 해당 업체 주문 삭제 가능
    this.checkIfValidAccessToModify(userInfo, order.getReceivingCompanyId(), OrderErrorCode.FORBIDDEN);

    if (!order.isPossibleToDelete()) {
      throw new CustomException(OrderErrorCode.UNABLE_REMOVE);
    }
    order.softDelete(userInfo.userId());
  }
  
  @Transactional
  @Override
  public InternalConfirmOrderApplicationResponseDto internalConfirmOrderById(UUID orderId) {

    // 결제 서비스가 존재한다는 가정하에 진행
    Order order = orderRepository.findByIdAndStatusAndDeletedAtIsNull(orderId, OrderStatus.ACCEPTED)
        .orElseThrow(() -> new CustomException(OrderErrorCode.UNABLE_CONFIRM));

    // 1. 주문 확정
    order.confirm();
    // 2. 배송 생성 요청
    SaveDeliveryApplicationResponseDto responseDto = this.saveDelivery(order);
    order.assignDeliveryId(responseDto.deliveryId());

    return InternalConfirmOrderApplicationResponseDto.of(order.getId(), order.getStatus());
  }

  @Transactional
  @Override
  public InternalUpdateOrderStatusApplicationResponseDto internalUpdateOrderStatusById(
      UUID orderId, String status) {

    Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
        .orElseThrow(() -> new CustomException(OrderErrorCode.INVALID_ORDER_ID));

    order.updateStatus(OrderStatus.parse(status));
    return InternalUpdateOrderStatusApplicationResponseDto.of(
        order.getId(), order.getStatus().toString());
  }

  @Transactional(readOnly = true)
  @Override
  public IGetOrderDetailApplicationResponseDto internalGetOrderById(UUID orderId) {

    Order order = orderRepository.findByIdAndDeletedAtIsNullFetchJoin(orderId)
        .orElseThrow(() -> new CustomException(OrderErrorCode.INVALID_ORDER_ID));

    return IGetOrderDetailApplicationResponseDto.from(order);
  }

  @Transactional
  @Override
  public CancelDeliveryApplicationResponseDto cancelDeliveryByOrderId(UUID orderId) {

    Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
        .orElseThrow(() -> new CustomException(OrderErrorCode.UNABLE_CANCEL));

    return this.cancelDelivery(order.getDeliveryId());
  }

  @Transactional
  @Override
  public void rollbackUpdateStocksByOrderId(UUID orderId) {

    Order order = orderRepository.findByIdAndDeletedAtIsNullFetchJoin(orderId)
        .orElseThrow(() -> new CustomException(OrderErrorCode.INVALID_ORDER_ID));

    UpdateStocksApplicationResponseDto updateStocksApplicationResponseDto =
        productClient.updateStocks(
            UpdateStocksApplicationRequestDto.fromForRollback(order.getOrderItems()));
  }

  @Transactional
  @Override
  public void rollbackCancelDeliveryByOrderId(UUID orderId) {
    Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
        .orElseThrow(() -> new CustomException(OrderErrorCode.INVALID_ORDER_ID));

    this.rollbackCancelDelivery(order.getDeliveryId());
  }

  @Override
  public SaveDeliveryApplicationResponseDto saveDelivery(Order order) {
    SaveDeliveryApplicationResponseDto responseDto =
        deliveryClient.saveDelivery(SaveDeliveryApplicationRequestDto.of(
            order.getId(), order.getSupplierCompanyId(), order.getReceivingCompanyId()));
    return responseDto;
  }
  
  @Override
  public UpdateStocksApplicationResponseDto updateStocks(Map<UUID, Integer> productStockMap) {

    return productClient.updateStocks(UpdateStocksApplicationRequestDto.from(productStockMap));
  }

  @Override
  public UpdateStocksApplicationResponseDto decreaseStocks(Map<UUID, Integer> productStockMap) {

    return productClient.decreaseStocks(UpdateStocksApplicationRequestDto.from(productStockMap));
  }

  @Override
  public UpdateStocksApplicationResponseDto increaseStocks(Map<UUID, Integer> productStockMap) {

    return productClient.increaseStocks(UpdateStocksApplicationRequestDto.from(productStockMap));
  }
  
  @Override
  public CancelDeliveryApplicationResponseDto cancelDelivery(UUID deliveryId) {

    return deliveryClient.cancelDelivery(deliveryId);
  }

  @Override
  public void rollbackCancelDelivery(UUID deliveryId) {

    RollbackCancelDeliveryApplicationResponseDto rollbackCancelDeliveryResponse =
        deliveryClient.rollbackCancelDelivery(deliveryId);
  }

  private void validateOrderRequest(
      SaveOrderApplicationRequestDto SaveOrderDto, Map<UUID, Integer> productStocksMap) {

    GetProductsApplicationResponseDto productsDto = productClient.getProducts(productStocksMap.keySet());
    Set<UUID> hubIds = new HashSet<>();
    for (SaveOrderItemApplicationRequestDto orderItem : SaveOrderDto.orderItems()) {
      GetProductApplicationResponseDto productDto = productsDto.products()
          .get(orderItem.productId());

      if (productDto.quantity() < productStocksMap.get(orderItem.productId())) {
        throw new CustomException(OrderErrorCode.NOT_ENOUGH_STOCK);
      }
      if (!productDto.name().equals(orderItem.name())) {
        throw new CustomException(OrderErrorCode.NOT_VALID_PRD_NAME);
      }
      if (productDto.price().compareTo(orderItem.price()) != 0) {
        throw new CustomException(OrderErrorCode.NOT_VALID_PRD_PRICE);
      }
      hubIds.add(productDto.hubId());
    }

    // 하나의 주문은 하나의 허브만 가질 수 있다.
    if (hubIds.size() > 1) {
      throw new CustomException(OrderErrorCode.MULTIPLE_HUB);
    }
  }

  private void checkIfValidAccessToModify(CurrentUserInfoDto userInfo, UUID receivingCompanyId, OrderErrorCode code) {

    GetCompanyApplicationResponseDto company = null;
    if (UserRole.ROLE_COMPANY == userInfo.role()) {
      company = companyClient.getCompanyByCompanyManagerId(userInfo.userId());
    }
    if (UserRole.ROLE_MASTER == userInfo.role()) {
      company = companyClient.getCompanyByCompanyId(receivingCompanyId);
    }
    if (company == null || !company.id().equals(receivingCompanyId)) {
      throw new CustomException(code);
    }
  }

  private GetCompanyApplicationResponseDto getCompanyDtoByUser(CurrentUserInfoDto userInfo) {

    if (UserRole.ROLE_COMPANY == userInfo.role()) {
      return companyClient.getCompanyByCompanyManagerId(userInfo.userId());
    }
    return null;
  }
}
