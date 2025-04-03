package com.faster.product.app.product.application.dto.request;

import com.faster.product.app.product.application.event.UpdateStocksEvent;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Builder;

@Builder
public record UpdateStocksDBApplicationRequestDto(
    List<UpdateStockApplicationRequestDto> updateStockDBRequests
) {

  public static UpdateStocksDBApplicationRequestDto from(UpdateStocksEvent stocksEvent) {

    return UpdateStocksDBApplicationRequestDto.builder()
        .updateStockDBRequests(
            stocksEvent.updateStockEvents().stream()
                    .map(event ->
                        UpdateStockApplicationRequestDto.builder()
                            .id(event.productId())
                            .quantity(event.stock())
                            .build()
                    )
                .toList()
        )
        .build();
  }

  public SortedMap<UUID, Integer> toSortedStocksMap() {

    return this.updateStockDBRequests()
        .stream()
        .collect(Collectors.toMap(
            UpdateStocksDBApplicationRequestDto.UpdateStockApplicationRequestDto::id,
            UpdateStocksDBApplicationRequestDto.UpdateStockApplicationRequestDto::quantity,
            Integer::sum,
            TreeMap::new
        ));
  }

  @Builder
  public record UpdateStockApplicationRequestDto(
      UUID id,
      Integer quantity
  ) {
  }
}
