package com.faster.order.app.order.presentation;

import com.common.aop.annotation.AuthCheck;
import com.common.resolver.dto.UserRole;
import com.common.response.ApiResponse;
import com.faster.order.app.order.application.facade.OrderFacade;
import com.faster.order.app.order.application.usecase.OrderService;
import com.faster.order.app.order.presentation.dto.request.InternalUpdateOrderStatusRequestDto;
import com.faster.order.app.order.presentation.dto.response.IGetOrderDetailResponseDto;
import com.faster.order.app.order.presentation.dto.response.InternalConfirmOrderResponseDto;
import com.faster.order.app.order.presentation.dto.response.InternalUpdateOrderStatusResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "주문(Internal)", description = "주문 조회 및 수정")
@RequiredArgsConstructor
@RequestMapping("/internal/orders")
@RestController
public class OrderInternalController {
  private final OrderService orderService;
  private final OrderFacade orderFacade;

  @Operation(summary = "주문 조회", description = "주문 조회 API 입니다.")
  @AuthCheck
  @GetMapping("/{orderId}")
  public ResponseEntity<ApiResponse<IGetOrderDetailResponseDto>> internalGetOrderById(
      @PathVariable UUID orderId) {

    return ResponseEntity.ok()
        .body(new ApiResponse<>(
            "주문 조회가 성공적으로 수행되었습니다.",
            HttpStatus.OK.value(),
            IGetOrderDetailResponseDto.from(orderService.internalGetOrderById(orderId))));
  }

  @Operation(summary = "주문 확정 수정", description = "주문 확정 수정 API 입니다.")
  @AuthCheck(roles = {UserRole.ROLE_MASTER, UserRole.ROLE_COMPANY})
  @PatchMapping("/{orderId}/confirm")
  public ResponseEntity<ApiResponse<InternalConfirmOrderResponseDto>> internalConfirmOrderById(
      @PathVariable UUID orderId) {

    return ResponseEntity.ok()
        .body(new ApiResponse<>(
            "주문 확정이 성공적으로 수행되었습니다.",
            HttpStatus.OK.value(),
            InternalConfirmOrderResponseDto.from(orderFacade.internalConfirmOrderById(orderId))));
  }

  @Operation(summary = "주문 상태 수정", description = "주문 상태 수정 API 입니다.")
  @AuthCheck
  @PatchMapping("/{orderId}/status")
  public ResponseEntity<ApiResponse<InternalUpdateOrderStatusResponseDto>> internalUpdateOrderStatusById(
      @PathVariable UUID orderId, @RequestBody InternalUpdateOrderStatusRequestDto requestDto) {

    return ResponseEntity.ok()
        .body(new ApiResponse<>(
            "주문 상태 변경이 성공적으로 수행되었습니다.",
            HttpStatus.OK.value(),
            InternalUpdateOrderStatusResponseDto.from(
                orderService.internalUpdateOrderStatusById(orderId, String.valueOf(requestDto.status())))));
  }
}
