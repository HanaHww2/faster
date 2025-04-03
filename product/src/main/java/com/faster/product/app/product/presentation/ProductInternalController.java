package com.faster.product.app.product.presentation;

import com.common.aop.annotation.AuthCheck;
import com.common.resolver.annotation.CurrentUserInfo;
import com.common.resolver.dto.CurrentUserInfoDto;
import com.common.resolver.dto.UserRole;
import com.common.response.ApiResponse;
import com.faster.product.app.product.application.usecase.ProductService;
import com.faster.product.app.product.presentation.dto.request.UpdateProductHubRequestDto;
import com.faster.product.app.product.presentation.dto.request.UpdateStocksRequestDto;
import com.faster.product.app.product.presentation.dto.response.UpdateProductHubResponseDto;
import com.faster.product.app.product.presentation.dto.response.UpdateStocksResponseDto;
import com.faster.product.app.product.presentation.dto.response.GetProductsResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "상품(Internal)", description = "상품 조회 및 수정")
@RequestMapping("/internal/products")
@RequiredArgsConstructor
@RestController
public class ProductInternalController {
  private final ProductService productService;

  @Operation(summary = "모든 상품 조회", description = "모든 상품 조회 API 입니다.")
  @AuthCheck(roles={UserRole.ROLE_MASTER, UserRole.ROLE_COMPANY})
  @GetMapping
  public ResponseEntity<ApiResponse<GetProductsResponseDto>> getProductList(
      @RequestParam Set<UUID> ids
  ) {

    return ResponseEntity.ok()
        .body(new ApiResponse<>(
            "상품 목록이 성공적으로 조회되었습니다.",
            HttpStatus.OK.value(),
            GetProductsResponseDto.from(productService.getProductList(ids))
        ));
  }

  @Operation(summary = "상품 수량 수정", description = "상품 수량 수정 API 입니다.")
  @AuthCheck(roles={UserRole.ROLE_MASTER, UserRole.ROLE_COMPANY})
  @PatchMapping("/stocks")
  public ResponseEntity<ApiResponse<UpdateStocksResponseDto>> updateProductStocks(
      @RequestBody @Valid UpdateStocksRequestDto requestDto
  ) {

    return ResponseEntity.ok()
        .body(new ApiResponse<>(
            "상품 재고가 성공적으로 수정되었습니다.",
            HttpStatus.OK.value(),
            UpdateStocksResponseDto.from(
                productService.updateProductStocksInternal(requestDto.toSortedApplicationRequestDto()))
        ));
  }

  @Operation(summary = "상품 허브 수정", description = "상품 허브 수정 API 입니다.")
  @AuthCheck(roles={UserRole.ROLE_MASTER, UserRole.ROLE_COMPANY})
  @PatchMapping("/hub")
  public ResponseEntity<ApiResponse<UpdateProductHubResponseDto>> updateProductHubByCompanyId(
      @CurrentUserInfo CurrentUserInfoDto userInfo,
      @RequestBody @Valid UpdateProductHubRequestDto requestDto
  ) {

    return ResponseEntity.ok()
        .body(new ApiResponse<>(
            "상품 허브가 성공적으로 수정되었습니다.",
            HttpStatus.OK.value(),
            UpdateProductHubResponseDto.from(
                productService.updateProductHubByCompanyIdInternal(userInfo, requestDto.toApplicationDto()))
        ));
  }

  @Operation(summary = "상품 재고 감소", description = "상품 재고 감소 API 입니다.")
  @AuthCheck(roles={UserRole.ROLE_MASTER, UserRole.ROLE_COMPANY})
  @PatchMapping("/stocks/decrease")
  public ResponseEntity<ApiResponse<UpdateStocksResponseDto>> decreaseProductStocks(
      @RequestBody @Valid UpdateStocksRequestDto requestDto
  ) {

    return ResponseEntity.ok()
        .body(new ApiResponse<>(
            "상품 재고가 성공적으로 수정되었습니다.",
            HttpStatus.OK.value(),
            UpdateStocksResponseDto.from(
                productService.decreaseProductStocksInternalRedis(requestDto.toApplicationRequestDto()))
        ));
  }

  @Operation(summary = "상품 재고 증가", description = "상품 재고 증가 API 입니다.")
  @AuthCheck(roles={UserRole.ROLE_MASTER, UserRole.ROLE_COMPANY})
  @PatchMapping("/stocks/increase")
  public ResponseEntity<ApiResponse<UpdateStocksResponseDto>> increaseProductStocks(
      @RequestBody @Valid UpdateStocksRequestDto requestDto
  ) {

    return ResponseEntity.ok()
        .body(new ApiResponse<>(
            "상품 재고가 성공적으로 수정되었습니다.",
            HttpStatus.OK.value(),
            UpdateStocksResponseDto.from(
                productService.increaseProductStocksInternalRedis(requestDto.toApplicationRequestDto()))
        ));
  }

  @Operation(summary = "상품 삭제", description = "상품 삭제 API 입니다.")
  @AuthCheck(roles={UserRole.ROLE_MASTER, UserRole.ROLE_COMPANY})
  @DeleteMapping
  public ResponseEntity<ApiResponse<Void>> deleteProductByCompanyId(
      @CurrentUserInfo CurrentUserInfoDto userInfo,
      @RequestParam(name="company-id") UUID companyId
  ) {

    productService.deleteProductByCompanyIdInternal(userInfo, companyId);
    return ResponseEntity.ok()
        .body(new ApiResponse<>(
            "해당 업체의 상품 목록이 성공적으로 삭제되었습니다.",
            HttpStatus.OK.value(),
            null
        ));
  }
}
