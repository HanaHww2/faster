package com.faster.order.app.order.infrastructure.persistence.jpa;


import static com.faster.order.app.global.utils.QuerydslUtil.nullSafeBuilder;
import static com.faster.order.app.order.domain.entity.QOrder.order;

import com.common.exception.CustomException;
import com.common.resolver.dto.UserRole;
import com.faster.order.app.global.exception.OrderErrorCode;
import com.faster.order.app.order.domain.criteria.SearchOrderCriteria;
import com.faster.order.app.order.domain.projection.SearchOrderProjection;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
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
public class OrderJpaRepositoryCustomImpl implements OrderJpaRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public Page<SearchOrderProjection> getOrdersByConditionAndCompanyId(
      Pageable pageable, SearchOrderCriteria condition, UUID companyId, UserRole role) {

    OrderSpecifier[] orderSpecifiers = createOrderSpecifiers(pageable.getSort());

    List<SearchOrderProjection> dtoList = queryFactory
        .select(Projections.constructor(SearchOrderProjection.class,
            order.id, order.supplierCompanyId, order.supplierCompanyName,
            order.receivingCompanyId, order.ordererInfo.receivingCompanyName,
            order.deliveryId, order.name, order.totalPrice, order.status,
            order.createdAt, order.updatedAt))
        .from(order)
        .leftJoin(order.ordererInfo)
        .where(
            role == UserRole.ROLE_MASTER
                ? searchMasterCondition(condition)
                : searchCondition(condition, companyId)
        )
        .orderBy(orderSpecifiers)
        .fetch();

    JPAQuery<Long> countQuery = queryFactory
        .select(order.count())
        .from(order)
        .where(
            role == UserRole.ROLE_MASTER
                ? searchMasterCondition(condition)
                : searchCondition(condition, companyId)
        );

    return PageableExecutionUtils.getPage(dtoList, pageable, () -> countQuery.fetchOne());
  }

  private BooleanBuilder searchMasterCondition(SearchOrderCriteria criteria) {
    return this.searchCondition(criteria, null)
        .and(criteria.checkIsDeleted());
  }


  private BooleanBuilder searchCondition(SearchOrderCriteria criteria, UUID companyId) {
    return criteria.likeSupplierCompanyName()
        .and(criteria.likeReceivingCompanyName())
        .and(criteria.likeName())
        .and(criteria.likeAddress())
        .and(criteria.likeContact())
        .and(criteria.eqStatus())
        .and(criteria.betweenTotalPrice())
        .and(criteria.betweenPeriod())
        .and(eqReceivingCompanyId(companyId));
  }

  private BooleanBuilder eqReceivingCompanyId(UUID companyId) {
    return nullSafeBuilder(() -> order.receivingCompanyId.eq(companyId));
  }

  private OrderSpecifier[] createOrderSpecifiers(Sort sort) {

    return sort.stream()
        .map(order -> {
          com.querydsl.core.types.Order direction = order.getDirection().isAscending()
              ? com.querydsl.core.types.Order.ASC
              : com.querydsl.core.types.Order.DESC;
          return Arrays.stream(SortType.values())
              .filter(enumValue -> enumValue.checkIfMatched(order.getProperty()))
              .findAny()
              .orElseThrow(() -> new CustomException(OrderErrorCode.INVALID_SORT_CONDITION))
              .getOrderSpecifier(direction);
        })
        .toArray(OrderSpecifier[]::new);
  }

  @RequiredArgsConstructor
  enum SortType {
    TOTALPRICE((direction) -> new OrderSpecifier<>(direction, order.totalPrice)),
    CREATEDAT((direction) -> new OrderSpecifier<>(direction, order.createdAt)),
    UPDATEDAT((direction) -> new OrderSpecifier<>(direction, order.updatedAt));

    private final Function<com.querydsl.core.types.Order, OrderSpecifier> typedOrderSpecifier;

    private OrderSpecifier<?> getOrderSpecifier(com.querydsl.core.types.Order direction) {
      return typedOrderSpecifier.apply(direction);
    }

    private boolean checkIfMatched(String property) {
      return this.name().equals(property
          .replaceAll("_", "")
          .toUpperCase());
    }
  }
}
