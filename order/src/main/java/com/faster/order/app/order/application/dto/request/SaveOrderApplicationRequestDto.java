package com.faster.order.app.order.application.dto.request;

import com.faster.order.app.order.domain.entity.Order;
import com.faster.order.app.order.domain.entity.OrderItem;
import com.faster.order.app.order.domain.entity.OrdererInfo;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Builder;

@Builder
public record SaveOrderApplicationRequestDto(
    UUID supplierCompanyId,
    String supplierCompanyName,
    UUID receivingCompanyId,
    String receivingCompanyName,
    String receivingCompanyAddress,
    String receivingCompanyContact,
    String request,
    List<SaveOrderItemApplicationRequestDto> orderItems
) {

  public Order toEntity() {

    Order order = Order.of(supplierCompanyId, receivingCompanyId, supplierCompanyName, request);

    OrdererInfo ordererInfo = OrdererInfo.of(receivingCompanyName, receivingCompanyAddress,
        receivingCompanyContact);
    order.linkOrdererInfo(ordererInfo);

    this.orderItems.stream()
        .map(it -> it.toEntityWithOrder(order))
        .toList();

    order.assignName();
    order.calcTotalPrice();
    return order;
  }

  public Map<UUID, Integer> toProductStocksMap() {
    return this.orderItems()
        .stream()
        .collect(
            Collectors.toMap(
                SaveOrderItemApplicationRequestDto::productId,
                SaveOrderItemApplicationRequestDto::quantity,
                Integer::sum
            )
        );
  }

  public Map<UUID, Integer> toProductStocksMapForRollback() {
    return this.orderItems()
        .stream()
        .collect(
            Collectors.toMap(
                SaveOrderItemApplicationRequestDto::productId,
                item -> - item.quantity(),
                Integer::sum
            )
        );
  }

  @Builder
  public record SaveOrderItemApplicationRequestDto(
      UUID productId,
      String name,
      BigDecimal price,
      Integer quantity
  ) {

    OrderItem toEntityWithOrder(Order order) {
      OrderItem orderItem = this.toEntity();
      orderItem.linkWithOrder(order);
      return orderItem;
    }

    OrderItem toEntity() {
      return OrderItem.of(productId, name, price, quantity);
    }
  }
}
