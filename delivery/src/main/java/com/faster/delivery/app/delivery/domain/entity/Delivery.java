package com.faster.delivery.app.delivery.domain.entity;

import com.common.domain.BaseEntity;
import com.common.exception.CustomException;
import com.common.exception.type.ApiErrorCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "p_delivery")
@Entity
public class Delivery extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "order_id", nullable = false)
  private UUID orderId;

  @Column(name = "company_delivery_manager_id")
  private Long companyDeliveryManagerId;

  @Column(name = "source_hub_id", nullable = false)
  private UUID sourceHubId;

  @Column(name = "destination_hub_id", nullable = false)
  private UUID destinationHubId;

  @Column(name = "receipt_company_id", nullable = false)
  private UUID receiptCompanyId;

  @Column(name = "receip_company_address", nullable = false, length = 200)
  private String receiptCompanyAddress;

  @Column(name = "recipient_name", nullable = false, length = 100)
  private String recipientName;

  @Column(name = "recipient_slack_id", nullable = false, length = 200)
  private String recipientSlackId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status = Status.READY;

  @OneToMany(mappedBy = "delivery", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
  private List<DeliveryRoute> deliveryRouteList = new ArrayList<>();

  @RequiredArgsConstructor
  public enum Status {
    READY(0, (status) -> status.order >= 0 || status.order == -100),
    DISPATCHED(1, (status) -> status.order >= 1), // 배송 시작
    INPROGRESS(2, (status) -> status.order >= 2), // 진행중
    INPROGRESS_TO_COMPANY(3, (status) -> status.order >= 3), // 진행중
    DELIVERED(4, (status) -> status.order >= 4),  // 배달 완료
    CANCELED(-100, (status) -> false),;

    private final int order;
    private final Predicate<Status> transitionRule;

    public boolean isPossibleToUpdate(Status status) {
      return transitionRule.test(status);
    }

    public boolean isOrderUpdateRequired() {
      return this == Status.DELIVERED || this == Status.DISPATCHED;
    }

    public static Status fromString(String search) {
      if (search == null) {
        return null;
      }
      return Status.valueOf(search.toUpperCase());
    }
  }

  @Builder
  private Delivery(
      UUID orderId,
      Long companyDeliveryManagerId,
      UUID sourceHubId,
      UUID destinationHubId,
      UUID receiptCompanyId,
      String receiptCompanyAddress,
      String recipientName,
      String recipientSlackId,
      List<DeliveryRoute> deliveryRouteList,
      Status status) {
    this.orderId = orderId;
    this.companyDeliveryManagerId = companyDeliveryManagerId;
    this.sourceHubId = sourceHubId;
    this.destinationHubId = destinationHubId;
    this.receiptCompanyId = receiptCompanyId;
    this.receiptCompanyAddress = receiptCompanyAddress;
    this.recipientName = recipientName;
    this.recipientSlackId = recipientSlackId;
    this.deliveryRouteList = new ArrayList<>();
    this.status = status;

    if (this.status == null) { // default 값 지정
      this.status = Status.READY;
    }
  }

  public void addDeliveryRouteList(List<DeliveryRoute> deliveryRouteList) {
    for (DeliveryRoute deliveryRoute : deliveryRouteList) {
      deliveryRoute.bindDelivery(this);
    }
    this.deliveryRouteList.addAll(deliveryRouteList);
  }

  public void updateStatus(Status status) {
    if (!this.status.isPossibleToUpdate(status)) {
      throw new CustomException(ApiErrorCode.INVALID_REQUEST);
    }
    this.status = status;
  }

  public void updateCompanyDeliveryManagerId(Long companyDeliveryManagerId) {
    this.companyDeliveryManagerId = companyDeliveryManagerId;
  }

  public Optional<DeliveryRoute> findDeliveryRouteById(UUID targetDeliveryRouteId) {
    return this.deliveryRouteList
        .stream()
        .filter(deliveryRoute -> targetDeliveryRouteId.equals(deliveryRoute.getId()))
        .findFirst();
  }

  public void updateDeliveryRouteRealMeasurement(DeliveryRoute deliveryRoute, Long realDistanceM, Long realTimeMin) {
    deliveryRoute.updateRealMeasurement(realDistanceM, realTimeMin);
  }

  public void updateDeliveryRouteStatus(DeliveryRoute deliveryRoute, DeliveryRoute.Status status) {
    deliveryRoute.updateStatus(status);
  }

  public void updateDeliveryRouteManager(DeliveryRoute deliveryRoute, Long
      deliveryManagerId, String deliveryManagerName) {
    deliveryRoute.updateManager(deliveryManagerId, deliveryManagerName);
  }

  public void cancel() {

    if (!this.status.isPossibleToUpdate(Status.CANCELED)) {
      throw new CustomException(ApiErrorCode.INVALID_REQUEST);
    }
    this.status = Status.CANCELED;
    this.deliveryRouteList.stream().forEach(DeliveryRoute::cancel);
  }

  public void rollbackCancel() {
    if (this.status != Status.CANCELED) {
      throw new CustomException(ApiErrorCode.INVALID_REQUEST);
    }
    this.status = Status.READY;
    this.deliveryRouteList.stream().forEach(DeliveryRoute::rollbackCancel);
  }
}
