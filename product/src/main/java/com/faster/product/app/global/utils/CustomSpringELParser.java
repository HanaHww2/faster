package com.faster.product.app.global.utils;

import com.common.exception.CustomException;
import com.faster.product.app.global.exception.ProductErrorCode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class CustomSpringELParser {

  private CustomSpringELParser() {
    throw new IllegalStateException("Utility class");
  }

  private static final String DELIMITER = ":";

  public static List<String> getDynamicValues(
      final String[] spelExpressions,
      final String[] parameterNames,
      final Object[] args) {

    if (spelExpressions.length == 0) {
      return Collections.singletonList(DELIMITER);
    }

    final List<String> keys = Arrays.stream(spelExpressions)
        .map(spel -> getDynamicValue(spel, parameterNames, args).toString())
        .toList();

    if (keys.size() == 0) {
      throw new CustomException(ProductErrorCode.NON_LOCK_KEY);
    }

    return keys;
  }

  public static Object getDynamicValue(String spel, String[] parameterNames, Object[] args) {

    ExpressionParser parser = new SpelExpressionParser();
    StandardEvaluationContext context = new StandardEvaluationContext();

    for (int i = 0; i < parameterNames.length; i++) {
      context.setVariable(parameterNames[i], args[i]);
    }
    return parser.parseExpression(spel).getValue(context, Object.class);
  }
}