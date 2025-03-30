package com.faster.order.app.order.infrastructure;

import com.faster.order.app.order.application.client.ProductClient;
import com.faster.order.app.order.application.dto.response.GetProductsApplicationResponseDto;
import com.faster.order.app.order.application.dto.request.UpdateStocksApplicationRequestDto;
import com.faster.order.app.order.application.dto.response.UpdateStocksApplicationResponseDto;
import com.faster.order.app.order.infrastructure.feign.ProductFeignClient;
import com.faster.order.app.order.infrastructure.feign.dto.request.UpdateStocksRequestDto;
import com.faster.order.app.order.infrastructure.feign.dto.response.GetProductsResponseDto;
import com.faster.order.app.order.infrastructure.feign.dto.response.UpdateStocksResponseDto;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductClientImpl implements ProductClient {
  private final ProductFeignClient productFeignClient;

  @Override
  public GetProductsApplicationResponseDto getProducts(Set<UUID> ids) {
    GetProductsResponseDto responseDto = productFeignClient.getProducts(ids).getBody().data();
    return responseDto.toApplicationDto();
  }

  @Override
  public UpdateStocksApplicationResponseDto updateStocks(
      UpdateStocksApplicationRequestDto requests) {
    UpdateStocksResponseDto responseDto = productFeignClient.updateStocks(
        UpdateStocksRequestDto.from(requests)).getBody().data();
    return responseDto.toApplicationDto();
  }
}
