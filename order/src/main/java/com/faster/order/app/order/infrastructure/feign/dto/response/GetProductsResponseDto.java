package com.faster.order.app.order.infrastructure.feign.dto.response;

import com.faster.order.app.order.application.dto.response.GetProductsApplicationResponseDto;
import com.faster.order.app.order.application.dto.response.GetProductsApplicationResponseDto.GetProductApplicationResponseDto;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Builder;

@Builder
public record GetProductsResponseDto(
    Map<UUID, GetProductResponseDto> products
) {

  public GetProductsApplicationResponseDto toApplicationDto() {
    return GetProductsApplicationResponseDto.builder()
        .products(products.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey(),
                entry -> entry.getValue().toApplicationDto()
            ))
        )
        .build();
  }

  @Builder
  record GetProductResponseDto(
      UUID id,
      UUID hubId,
      UUID companyId,
      String companyName,
      String name,
      BigDecimal price,
      Integer quantity,
      String description
  ) {

    public GetProductApplicationResponseDto toApplicationDto() {
      return GetProductApplicationResponseDto.builder()
          .id(id)
          .hubId(hubId)
          .companyId(companyId)
          .companyName(companyName)
          .name(name)
          .price(price)
          .quantity(quantity)
          .description(description)
          .build();
    }
  }
}
