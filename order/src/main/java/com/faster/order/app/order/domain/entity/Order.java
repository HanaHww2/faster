package com.faster.order.app.order.domain.entity;

import com.common.domain.BaseEntity;
import com.common.exception.CustomException;
import com.faster.order.app.global.exception.OrderErrorCode;
import com.faster.order.app.order.domain.enums.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private UUID supplierCompanyId;

  @Column(nullable = false)
  private UUID receivingCompanyId;

  @Column
  private UUID deliveryId;

  @Column(nullable = false, length = 100)
  private String supplierCompanyName;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false)
  private BigDecimal totalPrice;

  @Column(length = 500)
  private String request;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status;

  @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "orderer_info_id")
  private OrdererInfo ordererInfo;

  @Embedded
  private OrderItems orderItems;

  @Builder
  private Order(UUID supplierCompanyId, UUID receivingCompanyId, UUID deliveryId,
      String supplierCompanyName, String name, BigDecimal totalPrice,
      String request, OrderStatus status, OrdererInfo ordererInfo, OrderItems orderItems) {
    this.supplierCompanyId = supplierCompanyId;
    this.receivingCompanyId = receivingCompanyId;
    this.deliveryId = deliveryId;
    this.supplierCompanyName = supplierCompanyName;
    this.name = name;
    this.totalPrice = totalPrice;
    this.request = request;
    this.status = status;
    this.ordererInfo = ordererInfo;
    this.orderItems = orderItems;
  }

  public static Order of(UUID supplierCompanyId, UUID receivingCompanyId,
      String supplierCompanyName, String request) {
    return Order.builder()
        .supplierCompanyId(supplierCompanyId)
        .receivingCompanyId(receivingCompanyId)
        .supplierCompanyName(supplierCompanyName)
        .request(request)
        .status(OrderStatus.ACCEPTED)
        .orderItems(new OrderItems())
        .build();
  }

  public static Order of(UUID supplierCompanyId, UUID receivingCompanyId, UUID deliveryId,
      String supplierCompanyName, String request, OrderStatus status) {
    return Order.builder()
        .supplierCompanyId(supplierCompanyId)
        .receivingCompanyId(receivingCompanyId)
        .deliveryId(deliveryId)
        .supplierCompanyName(supplierCompanyName)
        .request(request)
        .status(status)
        .orderItems(new OrderItems())
        .build();

  }

  public void assignName() {
    this.name = this.orderItems.getOrderName();
  }

  public void calcTotalPrice() {
    this.totalPrice = this.orderItems.getTotalPrice();

    if (!isValidTotalPrice()) {
      throw new CustomException(OrderErrorCode.NOT_ENOUGH_TOTAL_PRICE);
    }
  }

  private boolean isValidTotalPrice() {
    return this.totalPrice.compareTo(BigDecimal.valueOf(3000)) >= 0;
  }

  public boolean isPossibleToDelete() {
    return Set.of(OrderStatus.COMPLETED, OrderStatus.CANCELED).contains(this.status);
  }

  public void cancel() {
    this.status = OrderStatus.CANCELED;
  }

  public void confirm() {
    this.status = OrderStatus.CONFIRMED;
  }

  public void updateStatus(OrderStatus status) {

    // 1. 접수, 취소 상태로는 업데이트 할 수 없다.
    // 2. 상태는 이전 상태로 되돌아 갈 수 없다.
    if (!status.isValidToUpdate() || !status.isPossibleToProceed(this.status)) {
      throw new CustomException(OrderErrorCode.INVALID_STATUS);
    }
    this.status = status;
  }

  public void linkOrdererInfo(OrdererInfo ordererInfo) {
    ordererInfo.linkOrder(this);
    this.ordererInfo = ordererInfo;
  }

  public void softDelete(Long userId) {
    LocalDateTime now = LocalDateTime.now();
    this.ordererInfo.softDelete(now, userId);
    this.orderItems.softDelete(now, userId);
    super.delete(now, userId);
  }

  public void assignDeliveryId(UUID deliveryId) {
    this.deliveryId = deliveryId;
  }
}
