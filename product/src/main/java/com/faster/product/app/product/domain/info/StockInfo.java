package com.faster.product.app.product.domain.info;

import com.faster.product.app.product.domain.entity.Product;
import java.util.UUID;
import lombok.Builder;

@Builder
public record StockInfo(
   UUID productId,
   Integer quantity
) {

  public static StockInfo from(Product product) {
    return StockInfo.builder()
        .productId(product.getId())
        .quantity(product.getQuantity())
        .build();
  }
}
