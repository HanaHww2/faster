package com.faster.order.app.global.exception;

import com.common.exception.type.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum OrderErrorCode implements ErrorCode {

  NOT_ENOUGH_TOTAL_PRICE(
      HttpStatus.UNPROCESSABLE_ENTITY.value(),
      "결제 금액이 최소 결제 금액 3000원보다 적습니다.",
      HttpStatus.UNPROCESSABLE_ENTITY),
  INVALID_ORDER_ID(
      HttpStatus.NOT_FOUND.value(),
      "존재하지 않는 주문 아이디입니다.",
      HttpStatus.NOT_FOUND),
  UNABLE_REMOVE(
      HttpStatus.UNPROCESSABLE_ENTITY.value(),
      "주문은 취소, 완료 상태만 삭제 가능합니다.",
      HttpStatus.UNPROCESSABLE_ENTITY),
  INVALID_SORT_CONDITION(
      HttpStatus.UNPROCESSABLE_ENTITY.value(),
      "주문의 정렬 정보가 유효하지 않습니다.",
      HttpStatus.UNPROCESSABLE_ENTITY),
  INVALID_PERIOD(
      HttpStatus.UNPROCESSABLE_ENTITY.value(),
      "주문의 검색 기간이 유효하지 않습니다.",
      HttpStatus.UNPROCESSABLE_ENTITY),
  UNABLE_CANCEL(
      HttpStatus.UNPROCESSABLE_ENTITY.value(),
      "주문이 취소 가능한 상태가 아닙니다.",
      HttpStatus.UNPROCESSABLE_ENTITY),
  UNABLE_CONFIRM(
      HttpStatus.UNPROCESSABLE_ENTITY.value(),
      "주문이 확정 가능한 상태가 아닙니다.",
      HttpStatus.UNPROCESSABLE_ENTITY),
  INVALID_STATUS(
      HttpStatus.UNPROCESSABLE_ENTITY.value(),
      "변경 가능한 주문 상태 값이 아닙니다.",
      HttpStatus.UNPROCESSABLE_ENTITY),
  NOT_ENOUGH_STOCK(
      HttpStatus.UNPROCESSABLE_ENTITY.value(),
      "주문 상품의 재고가 충분하지 않습니다.",
      HttpStatus.UNPROCESSABLE_ENTITY),
  NOT_VALID_PRD_NAME(
      HttpStatus.UNPROCESSABLE_ENTITY.value(),
      "주문 상품의 이름이 일치하지 않습니다.",
      HttpStatus.UNPROCESSABLE_ENTITY),
  NOT_VALID_PRD_PRICE(
      HttpStatus.UNPROCESSABLE_ENTITY.value(),
      "주문 상품의 가격이 일치하지 않습니다.",
      HttpStatus.UNPROCESSABLE_ENTITY),
  MULTIPLE_HUB(
          HttpStatus.UNPROCESSABLE_ENTITY.value(),
      "주문에 속한 주문 상품들의 허브가 여러 곳일 수 없습니다.",
      HttpStatus.UNPROCESSABLE_ENTITY),
  FORBIDDEN(
      HttpStatus.FORBIDDEN.value(),
      "해당 주문에 대한 접근 권한이 없습니다.",
      HttpStatus.FORBIDDEN),
  FORBIDDEN_SAVE(
      HttpStatus.FORBIDDEN.value(),
      "해당 업체의 주문 생성 대한 접근 권한이 없습니다.",
      HttpStatus.FORBIDDEN),
  FAIL_CANCEL_ORDER(
      HttpStatus.UNPROCESSABLE_ENTITY.value(),
      "주문 취소 처리에 실패하였습니다.",
      HttpStatus.UNPROCESSABLE_ENTITY),
  ;

  private final int code;
  private final String message;
  private final HttpStatus status;
}
