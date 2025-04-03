package com.faster.product.app.product.application.usecase;

import com.common.resolver.dto.CurrentUserInfoDto;
import com.common.response.PageResponse;
import com.faster.product.app.product.application.dto.request.GetProductsApplicationResponseDto;
import com.faster.product.app.product.application.dto.request.SearchProductConditionDto;
import com.faster.product.app.product.application.dto.request.SortedUpdateStocksApplicationRequestDto;
import com.faster.product.app.product.application.dto.request.UpdateProductApplicationRequestDto;
import com.faster.product.app.product.application.dto.request.UpdateProductHubApplicationRequestDto;
import com.faster.product.app.product.application.dto.request.UpdateStocksApplicationRequestDto;
import com.faster.product.app.product.application.dto.request.UpdateStocksDBApplicationRequestDto;
import com.faster.product.app.product.application.dto.response.SearchProductApplicationResponseDto;
import com.faster.product.app.product.application.dto.response.UpdateProductHubApplicationResponseDto;
import com.faster.product.app.product.application.dto.response.UpdateStocksApplicationResponseDto;
import com.faster.product.app.product.application.dto.response.GetProductDetailApplicationResponseDto;
import com.faster.product.app.product.application.dto.response.UpdateProductApplicationResponseDto;
import com.faster.product.app.product.application.dto.request.SaveProductApplicationRequestDto;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface ProductService {

  GetProductDetailApplicationResponseDto getProductById(UUID productId);

  void deleteProductById(CurrentUserInfoDto userInfo, UUID productId);

  UpdateProductApplicationResponseDto updateProductById(
      CurrentUserInfoDto userInfo, UUID productId, UpdateProductApplicationRequestDto requestDto);

  UUID saveProduct(CurrentUserInfoDto userInfo, SaveProductApplicationRequestDto applicationRequestDto);

  GetProductsApplicationResponseDto getProductList(Set<UUID> ids);

  UpdateStocksApplicationResponseDto updateProductStocksInternal(
      SortedUpdateStocksApplicationRequestDto applicationRequestDto);

  PageResponse<SearchProductApplicationResponseDto> getProductsByCondition(CurrentUserInfoDto userInfo, Pageable pageable, SearchProductConditionDto of);

  UpdateProductHubApplicationResponseDto updateProductHubByCompanyIdInternal(
      CurrentUserInfoDto userInfo, UpdateProductHubApplicationRequestDto applicationDto);

  void deleteProductByCompanyIdInternal(CurrentUserInfoDto userInfo, UUID companyId);

  UpdateStocksApplicationResponseDto decreaseProductStocksInternalRedis(
      UpdateStocksApplicationRequestDto updateStocksDto);

  UpdateStocksApplicationResponseDto increaseProductStocksInternalRedis(
      UpdateStocksApplicationRequestDto updateStocksDto);

  void updateStocksEvent(UpdateStocksDBApplicationRequestDto from);
}
