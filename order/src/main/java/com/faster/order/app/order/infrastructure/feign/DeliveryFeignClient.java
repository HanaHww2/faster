package com.faster.order.app.order.infrastructure.feign;

import com.common.response.ApiResponse;
import com.faster.order.app.order.infrastructure.feign.dto.request.CancelDeliveryRequestDto;
import com.faster.order.app.order.infrastructure.feign.dto.request.SaveDeliveryRequestDto;
import com.faster.order.app.order.infrastructure.feign.dto.response.CancelDeliveryResponseDto;
import com.faster.order.app.order.infrastructure.feign.dto.response.RollbackCancelDeliveryResponseDto;
import com.faster.order.app.order.infrastructure.feign.dto.response.SaveDeliveryResponseDto;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "delivery-service")
public interface DeliveryFeignClient {

  @PostMapping("/internal/deliveries")
  ResponseEntity<ApiResponse<SaveDeliveryResponseDto>> saveDelivery(
      @RequestBody SaveDeliveryRequestDto requestDto);

  @PatchMapping("/internal/deliveries/{deliveryId}/cancel")
  ResponseEntity<ApiResponse<CancelDeliveryResponseDto>> cancelDelivery(@PathVariable UUID deliveryId);

  @PatchMapping("/internal/deliveries/{deliveryId}/cancel/rollback")
  ResponseEntity<ApiResponse<RollbackCancelDeliveryResponseDto>> rollbackCancelDelivery(@PathVariable UUID deliveryId);
}
