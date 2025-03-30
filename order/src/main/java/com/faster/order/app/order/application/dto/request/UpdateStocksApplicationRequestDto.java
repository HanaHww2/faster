package com.faster.order.app.order.application.dto.request;

import com.faster.order.app.order.domain.entity.OrderItem;
import com.faster.order.app.order.domain.entity.OrderItems;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UpdateStocksApplicationRequestDto(
    List<UpdateStockApplicationRequestDto> updateStockRequests
) {

  public static UpdateStocksApplicationRequestDto from(OrderItems orderItems) {

    return UpdateStocksApplicationRequestDto.builder()
        .updateStockRequests(
            orderItems.stream()
                .map(UpdateStockApplicationRequestDto::from)
                .toList()
        )
        .build();
  }

  public static UpdateStocksApplicationRequestDto fromForRollback(OrderItems orderItems) {

    return UpdateStocksApplicationRequestDto.builder()
        .updateStockRequests(
            orderItems.stream()
                .map(UpdateStockApplicationRequestDto::fromForRollback)
                .toList()
        )
        .build();
  }

  public static UpdateStocksApplicationRequestDto of(List<UpdateStockApplicationRequestDto> updateStockDtoList) {
    return UpdateStocksApplicationRequestDto.builder()
        .updateStockRequests(updateStockDtoList)
        .build();
  }

  public static UpdateStocksApplicationRequestDto from(Map<UUID, Integer> productStocksMap) {
    return UpdateStocksApplicationRequestDto.builder()
        .updateStockRequests(productStocksMap.entrySet().stream()
            .map(item -> UpdateStockApplicationRequestDto.of(item.getKey(), item.getValue())
            )
            .toList())
        .build();
  }

  @Builder
  public record UpdateStockApplicationRequestDto(
      UUID id,
      Integer quantity
  ) {

    public static UpdateStockApplicationRequestDto from(OrderItem orderItem) {
      return UpdateStockApplicationRequestDto.builder()
          .id(orderItem.getId())
          .quantity(-orderItem.getQuantity())
          .build();
    }

    public static UpdateStockApplicationRequestDto fromForRollback(OrderItem orderItem) {
      return UpdateStockApplicationRequestDto.builder()
          .id(orderItem.getId())
          .quantity(orderItem.getQuantity())
          .build();
    }

    public static UpdateStockApplicationRequestDto of(UUID id, Integer quantity) {
      return UpdateStockApplicationRequestDto.builder()
          .id(id)
          .quantity(quantity)
          .build();
    }
  }
}
