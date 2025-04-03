package com.faster.order.app.order.infrastructure.feign;

import com.common.response.ApiResponse;
import com.faster.order.app.order.infrastructure.feign.dto.request.UpdateStocksRequestDto;
import com.faster.order.app.order.infrastructure.feign.dto.response.GetProductsResponseDto;
import com.faster.order.app.order.infrastructure.feign.dto.response.UpdateStocksResponseDto;
import java.util.Set;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service")
public interface ProductFeignClient {

  @GetMapping("/internal/products")
  ResponseEntity<ApiResponse<GetProductsResponseDto>> getProducts(@RequestParam Set<UUID> ids);

  @PatchMapping("/internal/products/stocks")
  ResponseEntity<ApiResponse<UpdateStocksResponseDto>> updateStocks(
      @RequestBody UpdateStocksRequestDto requests);

  @PatchMapping("/internal/products/stocks/decrease")
  ResponseEntity<ApiResponse<UpdateStocksResponseDto>> decreaseStocks(
      @RequestBody UpdateStocksRequestDto requests);

  @PatchMapping("/internal/products/stocks/increase")
  ResponseEntity<ApiResponse<UpdateStocksResponseDto>> increaseStocks(
      @RequestBody UpdateStocksRequestDto requests);
}
