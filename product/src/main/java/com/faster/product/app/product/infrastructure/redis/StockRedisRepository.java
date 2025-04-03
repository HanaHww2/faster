package com.faster.product.app.product.infrastructure.redis;

import com.faster.product.app.product.domain.info.StockInfo;
import java.time.Duration;

public interface StockRedisRepository {

  void setWithTtl(String key, Integer value);

  Integer getByKey(String key);

  Integer getAndExpireByKey(String key);

  boolean expireByKey(String key);

  Boolean deleteByKey(String key);

  Boolean hasKey(String key);

  Duration getRemainingExpirationByKey(String key);

  Long decrementByKey(String string, Integer value);

  Long incrementByKey(String string, Integer value);
}
