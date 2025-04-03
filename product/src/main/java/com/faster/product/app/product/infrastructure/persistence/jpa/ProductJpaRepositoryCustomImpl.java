package com.faster.product.app.product.infrastructure.persistence.jpa;

import static com.faster.product.app.global.utils.QuerydslUtil.nullSafeBuilder;
import static com.faster.product.app.product.domain.entity.QProduct.product;

import com.common.exception.CustomException;
import com.common.resolver.dto.UserRole;
import com.faster.product.app.global.exception.ProductErrorCode;
import com.faster.product.app.product.domain.criteria.SearchProductCriteria;
import com.faster.product.app.product.domain.entity.Product;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;


@RequiredArgsConstructor
public class ProductJpaRepositoryCustomImpl implements ProductJpaRepositoryCustom {
  private final JPAQueryFactory queryFactory;

  @Override
  public Page<Product> getProductsByConditionAndCompanyId(
      Pageable pageable, SearchProductCriteria condition, UUID companyId, UserRole role) {

    OrderSpecifier[] orderSpecifiers = createOrderSpecifiers(pageable.getSort());

    List<Product> dtoList = queryFactory
        .selectFrom(product)
        .where(
            role == UserRole.ROLE_MASTER
                ? searchMasterCondition(condition)
                : searchCondition(condition, companyId)
        )
        .orderBy(orderSpecifiers)
        .fetch();

    JPAQuery<Long> countQuery = queryFactory
        .select(product.count())
        .from(product)
        .where(
            role == UserRole.ROLE_MASTER
                ? searchMasterCondition(condition)
                : searchCondition(condition, companyId)
        );

    return PageableExecutionUtils.getPage(dtoList, pageable, () -> countQuery.fetchOne());
  }

  private BooleanBuilder searchMasterCondition(SearchProductCriteria criteria) {
    return this.searchCondition(criteria, null)
        .and(criteria.checkIsDeleted());
  }

  private BooleanBuilder searchCondition(SearchProductCriteria criteria, UUID companyId) {
    return criteria.likeCompanyName()
        .and(criteria.likeName())
        .and(criteria.betweenPrice())
        .and(criteria.betweenPeriod())
        .and(criteria.eqCompanyId());
  }

  private BooleanBuilder eqCompanyId(UUID companyId) {
    return nullSafeBuilder(() -> product.companyId.eq(companyId));
  }

  private OrderSpecifier[] createOrderSpecifiers(Sort sort) {

    return sort.stream()
        .map(order -> {
          Order direction = order.getDirection().isAscending() ? Order.ASC : Order.DESC;
          return Arrays.stream(SortType.values())
              .filter(enumValue -> enumValue.checkIfMatched(order.getProperty()))
              .findAny()
              .orElseThrow(() -> new CustomException(ProductErrorCode.INVALID_SORT_CONDITION))
              .getOrderSpecifier(direction);
        })
        .toArray(OrderSpecifier[]::new);
  }

  @RequiredArgsConstructor
  enum SortType {
    PRICE((direction) -> new OrderSpecifier<>(direction, product.price)),
    CREATEDAT((direction) -> new OrderSpecifier<>(direction, product.createdAt)),
    UPDATEDAT((direction) -> new OrderSpecifier<>(direction, product.updatedAt));

    private final Function<Order, OrderSpecifier> typedOrderSpecifier;

    private OrderSpecifier<?> getOrderSpecifier(Order direction) {
      return typedOrderSpecifier.apply(direction);
    }

    private boolean checkIfMatched(String property) {
      return this.name().equals(property
          .replaceAll("_", "")
          .toUpperCase());
    }
  }
}
