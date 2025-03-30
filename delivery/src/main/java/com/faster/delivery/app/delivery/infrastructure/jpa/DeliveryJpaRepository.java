package com.faster.delivery.app.delivery.infrastructure.jpa;

import com.faster.delivery.app.delivery.domain.entity.Delivery;
import com.faster.delivery.app.delivery.domain.repository.DeliveryRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DeliveryJpaRepository
    extends JpaRepository<Delivery, UUID>, DeliveryRepositoryCustom, DeliveryRepository {

  @Query("select d from Delivery d join fetch d.deliveryRouteList "
      + "where d.id = :deliveryId and d.deletedAt is null")
  Optional<Delivery> findByIdAndDeletedAtIsNullFetchJoin(UUID deliveryId);
}
