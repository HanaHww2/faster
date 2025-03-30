package com.faster.order.app.order.application.client;

import com.faster.order.app.order.application.dto.response.GetProductsApplicationResponseDto;
import com.faster.order.app.order.application.dto.request.UpdateStocksApplicationRequestDto;
import com.faster.order.app.order.application.dto.response.UpdateStocksApplicationResponseDto;
import java.util.Set;
import java.util.UUID;

public interface ProductClient {

  GetProductsApplicationResponseDto getProducts(Set<UUID> ids);

  UpdateStocksApplicationResponseDto updateStocks(UpdateStocksApplicationRequestDto requests);
}
