package com.faster.product.app.product.domain.repository;

import com.common.resolver.dto.UserRole;
import com.faster.product.app.product.domain.criteria.SearchProductCriteria;
import com.faster.product.app.product.domain.entity.Product;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductRepository {

  Optional<Product> findByIdAndDeletedAtIsNull(UUID productId);

  List<Product> findByIdInAndDeletedAtIsNull(Set<UUID> ids);

  Optional<Product> findByIdAndDeletedAtIsNullWithPessimisticLock(UUID productId);

  Page<Product> getProductsByConditionAndCompanyId(
      Pageable pageable, SearchProductCriteria criteria, UUID companyId, UserRole role);

  Product save(Product product);

  <S extends Product> List<S> saveAll(Iterable<S> entities);

  void updateProductHubByCompanyId(UUID companyId, UUID hubId, Long userId);

  void deleteProductByCompanyId(UUID companyId, Long userId);

  Integer getStock(UUID productId);

  Long decreaseStockByKey(UUID productId, Integer value);

  Long increaseStockByKey(UUID productId, Integer value);
}
