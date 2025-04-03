package com.faster.product.app.product.application.event;

import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record UpdateStocksEvent(
  List<UpdateStockEvent> updateStockEvents
) {

  public static UpdateStocksEvent of(List<UpdateStockEvent> stockEvents) {
    return UpdateStocksEvent.builder()
        .updateStockEvents(stockEvents)
        .build();
  }

  @Builder
  public record UpdateStockEvent(
      UUID productId,
      Integer stock
  ) {

    public static UpdateStockEvent of(UUID productId, Long stock) {
      return UpdateStockEvent.builder()
          .productId(productId)
          .stock(stock.intValue())
          .build();
    }
  }
}
