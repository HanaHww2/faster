package com.faster.delivery.app.delivery.domain.entity;

import com.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "p_delivery_route")
@Entity
public class DeliveryRoute extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @JoinColumn(name = "p_delivery_id", nullable = false)
  @ManyToOne(fetch = FetchType.LAZY)
  private Delivery delivery;

  @Column(name = "sequence", nullable = false)
  private Integer sequence;

  @Column(name = "source_hub_id", nullable = false)
  private UUID sourceHubId;

  @Column(name = "destination_hub_id", nullable = false)
  private UUID destinationHubId;

  @Column(name = "expected_distance_m", nullable = false)
  private Long expectedDistanceM;

  @Column(name = "expected_time_min", nullable = false)
  private Long expectedTimeMin;

  @Column(name = "real_distance_m")
  private Long realDistanceM;

  @Column(name = "real_time_min")
  private Long realTimeMin;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private Type type = Type.TO_HUB;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private Status status;

  @Column(name = "delivery_manager_id")
  private Long deliveryManagerId;

  @Column(name = "delivery_manager_name", length = 100)
  private String deliveryManagerName;

  public enum Type {
    TO_HUB,
    TO_COMPANY // 사용되는일 없을듯 : 업체배송은 Delivery.Status 를 보고 업체 배송 판단
  }

  public enum Status {
    PENDING,       // 대기중
    INPROGRESS,    //배송 중
    ARRIVED,       // 목적지 도착
    DELIVERED,     // 배송 완료
    CANCELLED     // 배송 취소
  }

  public void bindDelivery(Delivery delivery) {
    this.delivery = delivery;
  }

  public void updateRealMeasurement(Long realDistanceM, Long realTimeMin) {
    this.realDistanceM = realDistanceM;
    this.realTimeMin = realTimeMin;
  }

  public void updateStatus(Status status) {
    this.status = status;
  }

  public void updateManager(Long deliveryManagerId, String deliveryManagerName) {
    this.deliveryManagerId = deliveryManagerId;
    this.deliveryManagerName = deliveryManagerName;
  }

  public void cancel() {
    this.status = Status.CANCELLED;
  }

  public void rollbackCancel() {
    this.status = Status.PENDING;
  }
}
