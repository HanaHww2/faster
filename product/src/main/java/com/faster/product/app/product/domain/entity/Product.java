package com.faster.product.app.product.domain.entity;

import com.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "p_product")
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private UUID hubId;

  @Column(nullable = false)
  private UUID companyId;

  @Column(nullable = false, length = 100)
  private String companyName;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false)
  private BigDecimal price;

  @Column(nullable = false)
  private Integer quantity;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Builder
  private Product(UUID hubId, UUID companyId, String companyName, String name,
      BigDecimal price, Integer quantity, String description) {
    this.hubId = hubId;
    this.companyId = companyId;
    this.companyName = companyName;
    this.name = name;
    this.price = price;
    this.quantity = quantity;
    this.description = description;
  }

  public static Product of(UUID hubId, UUID companyId, String companyName, String name,
      BigDecimal price, Integer quantity, String description) {

    return Product.builder()
        .hubId(hubId)
        .companyId(companyId)
        .companyName(companyName)
        .name(name)
        .price(price)
        .quantity(quantity)
        .description(description)
        .build();
  }

  public void updateContent(String name, BigDecimal price, Integer quantity, String description) {
    this.name = name;
    this.price = price;
    this.quantity = quantity;
    this.description = description;
  }

  public void updateStockFinal(Integer quantity) {
    this.quantity = quantity;
  }

  public boolean updateStock(Integer quantity) {
    if (quantity > 0 && this.quantity < quantity) {
      return false;
    }
    this.quantity -= quantity;
    return true;
  }

  public void softDelete(Long userId) {
    LocalDateTime now = LocalDateTime.now();
    super.delete(now, userId);
  }
}
