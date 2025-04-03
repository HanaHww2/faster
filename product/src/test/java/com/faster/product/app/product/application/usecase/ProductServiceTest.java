package com.faster.product.app.product.application.usecase;


import static org.assertj.core.api.Assertions.assertThat;

import com.faster.product.app.product.application.dto.request.SortedUpdateStocksApplicationRequestDto;
import com.faster.product.app.product.application.dto.request.SortedUpdateStocksApplicationRequestDto.UpdateStockApplicationRequestDto;
import com.faster.product.app.product.application.dto.request.UpdateStocksApplicationRequestDto;
import com.faster.product.app.product.config.RedisTestContainerExtension;
import com.faster.product.app.product.domain.entity.Product;
import com.faster.product.app.product.domain.repository.ProductRepository;
import com.faster.product.app.product.fixture.ProductFixture;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import com.faster.product.app.product.utils.DatabaseCleanUp;

@Slf4j
@Import({DatabaseCleanUp.class})
@ExtendWith(RedisTestContainerExtension.class)
@ActiveProfiles("test")
@SpringBootTest
class ProductServiceTest {

  @Autowired
  private ProductService productService;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  DatabaseCleanUp databaseCleanUp;

  List<Product> products;

  @BeforeEach
  void setUp() {

    var hubId = UUID.randomUUID();
    var companyId = UUID.randomUUID();

    this.products = IntStream.range(0, 10)
        .mapToObj(i -> ProductFixture.createProduct(hubId, companyId, i)
        )
        .toList();
    productRepository.saveAll(products);
  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.afterPropertiesSet();
    databaseCleanUp.execute();
  }

  @DisplayName("재고 차감이 정상적으로 수행되는지 검증")
  @Test
  void updateProductStocks() throws InterruptedException {
    // given
    var updateStocksRequest = SortedUpdateStocksApplicationRequestDto.builder()
        .sortedUpdateStockRequests(createUpdateStockRequest(true))
        .build();
    int threadCount = 1000;
    ExecutorService executorService = Executors.newFixedThreadPool(32);
    CountDownLatch latch = new CountDownLatch(threadCount);

    // when
    for (int i = 0; i < threadCount; i++) {
      executorService.submit(() -> {
        try {
          productService.updateProductStocksInternal(updateStocksRequest);
        } catch (Exception e) {
          log.error(e.getMessage());
        } finally {
          latch.countDown();
        }
      });
    }
    latch.await();
    executorService.shutdown();

    // then
    Product product1 =
        productRepository.findByIdAndDeletedAtIsNull(products.get(0).getId()).orElseThrow();
    Product product2 =
        productRepository.findByIdAndDeletedAtIsNull(products.get(1).getId()).orElseThrow();

    assertThat(product1.getQuantity()).isEqualTo(0);
    assertThat(product2.getQuantity()).isEqualTo(0);
  }

  @DisplayName("양방향으로 재고 차감 요청이 들어올 때 정상적으로 수행되는지 검증-데드락 발생")
  @Test
  void updateProductStocksBiDir() throws InterruptedException {
    // given
    var updateStocksRequest = SortedUpdateStocksApplicationRequestDto.builder()
        .sortedUpdateStockRequests(createUpdateStockRequest(true))
        .build();
    var updateStocksRequestReversed = SortedUpdateStocksApplicationRequestDto.builder()
        .sortedUpdateStockRequests(createUpdateStockRequest(false))
        .build();
    int threadCount = 1000;
    ExecutorService executorService = Executors.newFixedThreadPool(32);
    CountDownLatch latch = new CountDownLatch(threadCount);

    // when
    for (int i = 0; i < threadCount; i++) {
      var isAscending = i % 2 == 0;
      executorService.submit(() -> {
        try {
          productService.updateProductStocksInternal(
              isAscending ? updateStocksRequest : updateStocksRequestReversed);
        } catch (Exception e) {
          log.error(e.getMessage());
        } finally {
          latch.countDown();
        }
      });
    }
    latch.await();
    executorService.shutdown();

    // then
    Product product1 =
        productRepository.findByIdAndDeletedAtIsNull(products.get(0).getId()).orElseThrow();
    Product product2 =
        productRepository.findByIdAndDeletedAtIsNull(products.get(1).getId()).orElseThrow();
    assertThat(product1.getQuantity()).isNotEqualTo(0);
    assertThat(product2.getQuantity()).isNotEqualTo(0);
  }

  private List<UpdateStockApplicationRequestDto> createUpdateStockRequest(boolean isAscending) {
    return List.of(
            UpdateStockApplicationRequestDto.builder()
                .id(products.get(0).getId())
                .quantity(1)
                .build(),
            UpdateStockApplicationRequestDto.builder()
                .id(products.get(1).getId())
                .quantity(1)
                .build()
        )
        .stream()
        .sorted(
            isAscending
                ? Comparator.comparing(UpdateStockApplicationRequestDto::id)
                : Comparator.comparing(UpdateStockApplicationRequestDto::id).reversed()
        )
        .toList();
  }

  @DisplayName("레디스 적용시 재고 차감이 정상적으로 수행되는지 검증")
  @Test
  void decreaseProductStocksRedis() throws InterruptedException {
    // given
    var updateStocksRequest = UpdateStocksApplicationRequestDto.builder()
        .updateStockRequests(
            List.of(
                UpdateStocksApplicationRequestDto.UpdateStockApplicationRequestDto.builder()
                    .id(products.get(0).getId())
                    .quantity(1)
                    .build(),
                UpdateStocksApplicationRequestDto.UpdateStockApplicationRequestDto.builder()
                    .id(products.get(1).getId())
                    .quantity(1)
                    .build()
            )
        )
        .build();
    int threadCount = 1000;
    ExecutorService executorService = Executors.newFixedThreadPool(32);
    CountDownLatch latch = new CountDownLatch(threadCount);

    // when
    for (int i = 0; i < threadCount; i++) {
      executorService.submit(() -> {
        try {
          productService.decreaseProductStocksInternalRedis(updateStocksRequest);
        } catch (Exception e) {
          log.error(e.getMessage());
        } finally {
          latch.countDown();
        }
      });
    }
    latch.await();
    executorService.shutdown();

    // then
    Integer stock1 =
        productRepository.getStock(products.get(0).getId());
    Integer stock2 =
        productRepository.getStock(products.get(1).getId());
    assertThat(stock1).isEqualTo(0);
    assertThat(stock2).isEqualTo(0);
  }
}