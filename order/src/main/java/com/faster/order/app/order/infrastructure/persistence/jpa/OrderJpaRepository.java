package com.faster.order.app.order.infrastructure.persistence.jpa;

import com.faster.order.app.order.domain.entity.Order;
import com.faster.order.app.order.domain.enums.OrderStatus;
import com.faster.order.app.order.domain.repository.OrderRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderJpaRepository extends JpaRepository<Order, UUID>, OrderJpaRepositoryCustom, OrderRepository {

  @Query("""
            select distinct o from Order o 
            join fetch o.orderItems.orderItems 
            join fetch o.ordererInfo 
            where o.id = :orderId and o.deletedAt is null""")
  Optional<Order> findByIdAndDeletedAtIsNullFetchJoin(UUID orderId);

  @Query("""
            select distinct o from Order o 
            join fetch o.orderItems.orderItems
            where o.id = :orderId and o.status = :status and o.deletedAt is null""")
  Optional<Order> findByIdAndStatusAndDeletedAtIsNullFetchJoin(UUID orderId, OrderStatus status);

}
