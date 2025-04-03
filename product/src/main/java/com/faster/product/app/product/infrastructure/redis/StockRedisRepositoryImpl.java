package com.faster.product.app.product.infrastructure.redis;

import com.faster.product.app.product.domain.info.StockInfo;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StockRedisRepositoryImpl implements StockRedisRepository {

  private static final String PREFIX = "product:stock:";
  private static final Duration TIMEOUT = Duration.ofMinutes(30);
  private final RedisTemplate<String, Object> redisTemplate;

  @Override
  public void setWithTtl(String key, Integer value) {
    redisTemplate.opsForValue().set(PREFIX + key, value, TIMEOUT);
  }

  @Override
  public Integer getByKey(String key) {
    return (Integer) redisTemplate.opsForValue().get(PREFIX + key);
  }

  @Override
  public Integer getAndExpireByKey(String key) {
    return (Integer) redisTemplate.opsForValue().getAndExpire(PREFIX + key, TIMEOUT);
  }

  @Override
  public boolean expireByKey(String key) {
    return redisTemplate.expire(PREFIX + key, TIMEOUT);
  }

  @Override
  public Boolean deleteByKey(String key) {
    return redisTemplate.delete(PREFIX + key);
  }

  @Override
  public Boolean hasKey(String key) {
    return redisTemplate.hasKey(PREFIX + key);
  }

  @Override
  public Duration getRemainingExpirationByKey(String key) {
    Long expire = redisTemplate.getExpire(PREFIX + key);
    if (expire != null && expire >= 0) {
      return Duration.ofSeconds(expire);
    } else {
      return null; // 유효 기간이 없거나 키가 존재하지 않는 경우
    }
  }

  @Override
  public Long decrementByKey(String key, Integer value) {
    return redisTemplate.opsForValue().decrement(PREFIX + key, value);
  }

  @Override
  public Long incrementByKey(String key, Integer value) {
    return redisTemplate.opsForValue().increment(PREFIX + key, value);
  }
}
