package com.faster.gateway.app.global.security.jwt.filter;

import com.faster.gateway.app.global.security.jwt.util.TokenProvider;
import jakarta.ws.rs.core.HttpHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

  private final TokenProvider tokenProvider;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String accessToken = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    log.info("[JwtAuthenticationFilter]{}", accessToken);

    if (tokenProvider.validAccessToken(accessToken)) {
      return tokenProvider.getAuthentication(accessToken)
          .flatMap(authentication ->
              chain.filter(exchange)
                  .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)));
    } else {
      return chain.filter(exchange); // 토큰이 유효하지 않은 경우 인증을 설정하지 않고 요청을 계속 진행
    }
  }
}
