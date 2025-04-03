package com.faster.product.app.global.aop;

import com.common.exception.CustomException;
import com.faster.product.app.global.aop.annotation.DistributedLock;
import com.faster.product.app.global.exception.ProductErrorCode;
import com.faster.product.app.global.utils.CustomSpringELParser;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class DistributedLockAop {
  private static final String REDISSON_LOCK_PREFIX = "LOCK:";

  private final RedissonClient redissonClient;

  @Around("@annotation(distributedLock)")
  public Object lock(final ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {

    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();

    List<String> values = CustomSpringELParser.getDynamicValues(distributedLock.keys(), signature.getParameterNames(), joinPoint.getArgs());
    List<String> keys = generateKeys(values);

    RLock rLock = null;
    if (keys.size() == 1) {
      rLock = redissonClient.getLock(keys.get(0));
    } else {
      rLock = redissonClient.getMultiLock(keys.stream().map(redissonClient::getLock).toArray(RLock[]::new));
    }

    try {
      boolean available = rLock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(),
          distributedLock.timeUnit());

      if (!available) {
        return false;
      }
      return joinPoint.proceed();

    } catch (InterruptedException e) {
      log.error("Redisson Lock 획득 실패 오류 {}", e.getMessage());
      throw new CustomException(ProductErrorCode.LOCK_PROBLEM);
    } finally {
      try {
        rLock.unlock();   // (4)
      } catch (IllegalMonitorStateException e) {
        log.info("Redisson Lock 이미 해제되어 오류 {}",  method.getName());
      }
    }
  }

  private List<String> generateKeys(final Collection<String> lockKeys) {
    return lockKeys.stream().map(key -> REDISSON_LOCK_PREFIX + key).toList();
  }
}
