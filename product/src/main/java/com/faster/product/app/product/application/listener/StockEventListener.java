package com.faster.product.app.product.application.listener;

import com.faster.product.app.product.application.dto.request.UpdateStocksDBApplicationRequestDto;
import com.faster.product.app.product.application.event.UpdateStocksEvent;
import com.faster.product.app.product.application.usecase.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class StockEventListener {
  private final ProductService productService;

  @Async
  @EventListener
  public void updateStocksResultEvent(UpdateStocksEvent event) {

    productService.updateStocksEvent(UpdateStocksDBApplicationRequestDto.from(event));
  }
}
