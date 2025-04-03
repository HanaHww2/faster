package com.faster.product.app.product.infrastructure;

import com.common.exception.CustomException;
import com.common.resolver.dto.UserRole;
import com.faster.product.app.global.exception.ProductErrorCode;
import com.faster.product.app.product.domain.criteria.SearchProductCriteria;
import com.faster.product.app.product.domain.entity.Product;
import com.faster.product.app.product.domain.repository.ProductRepository;
import com.faster.product.app.product.infrastructure.persistence.jpa.ProductJpaRepository;
import com.faster.product.app.product.infrastructure.redis.StockRedisRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
public class ProductRepositoryImpl implements ProductRepository {
  private final StockRedisRepository stockRedisRepository;
  private final ProductJpaRepository productJpaRepository;

  @Override
  public Optional<Product> findByIdAndDeletedAtIsNull(UUID productId) {
    return productJpaRepository.findByIdAndDeletedAtIsNull(productId);
  }

  @Override
  public Product save(Product product) {
    return productJpaRepository.save(product);
  }

  @Override
  public List<Product> findByIdInAndDeletedAtIsNull(Set<UUID> ids) {
    return productJpaRepository.findByIdInAndDeletedAtIsNull(ids);
  }

  @Override
  public Page<Product> getProductsByConditionAndCompanyId(Pageable pageable,
      SearchProductCriteria criteria, UUID companyId, UserRole role) {
    return productJpaRepository.getProductsByConditionAndCompanyId(pageable, criteria, companyId, role);
  }

  @Override
  public Optional<Product> findByIdAndDeletedAtIsNullWithPessimisticLock(UUID productId) {
    return productJpaRepository.findByIdAndDeletedAtIsNullWithPessimisticLock(productId);
  }

  @Override
  public <S extends Product> List<S> saveAll(Iterable<S> entities) {
    return productJpaRepository.saveAll(entities);
  }

  @Override
  public void updateProductHubByCompanyId(UUID companyId, UUID hubId, Long userId) {
    productJpaRepository.updateProductHubByCompanyId(companyId, hubId, userId);
  }

  @Override
  public void deleteProductByCompanyId(UUID companyId, Long userId) {
    productJpaRepository.deleteProductByCompanyId(companyId, userId);
  }

  @Override
  public Integer getStock(UUID productId) {
    Integer stock = stockRedisRepository.getAndExpireByKey(productId.toString());
    if (stock == null) {
      Product product = productJpaRepository.findByIdAndDeletedAtIsNull(productId)
          .orElseThrow(()-> new CustomException(ProductErrorCode.INVALID_ID));
      stock = product.getQuantity();
      stockRedisRepository.setWithTtl(productId.toString(), stock);
    }
    return stock;
  }

  @Override
  public Long decreaseStockByKey(UUID productId, Integer value) {
    Long result = stockRedisRepository.decrementByKey(productId.toString(), value);
    return result;
  }

  @Override
  public Long increaseStockByKey(UUID productId, Integer value) {
    Long result = stockRedisRepository.incrementByKey(productId.toString(), value);
    return result;
  }
}
