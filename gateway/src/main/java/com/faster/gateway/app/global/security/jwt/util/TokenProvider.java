package com.faster.gateway.app.global.security.jwt.util;

import com.common.exception.CustomException;
import com.faster.gateway.app.global.exception.GatewayErrorCode;
import com.faster.gateway.app.global.security.service.dto.CustomUserDetails;
import com.faster.gateway.app.global.security.service.dto.UserDetailsDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenProvider {

  public static final String TOKEN_PREFIX = "Bearer ";

  private final ReactiveUserDetailsService userDetailsService;

  private SecretKey key;
  @Value("${jwt.secret}")
  private String secretKey;

  public Mono<Authentication> getAuthentication(String token) {
    if (!StringUtils.hasText(token) || !token.startsWith(TOKEN_PREFIX)) {
      return null;
    }
    token = token.substring(TOKEN_PREFIX.length());

    UserDetailsDto userDetailsDto = UserDetailsDto.of(
        this.getUserId(token), this.getUserRole(token));
    CustomUserDetails userDetails = CustomUserDetails.builder().userDetailsDto(userDetailsDto).build();

    return Mono.fromSupplier(() ->
        new UsernamePasswordAuthenticationToken(
            userDetails,
            null,  // 비밀번호는 null로 설정
            userDetails.getAuthorities())
    );
  }

  public boolean validAccessToken(String token) {
    if (!StringUtils.hasText(token) || !token.startsWith(TOKEN_PREFIX)) {
      return false;
    }

    return validateToken(token.substring(TOKEN_PREFIX.length()));
  }

  private boolean validateToken(String token) {
    return !getExpiration(token).before(new Date());
  }

  private Date getExpiration(String token) {
    if (!StringUtils.hasText(token)) {
      throw CustomException.from(GatewayErrorCode.BAD_PADDING.EXPIRED_JWT_TOKEN);
    }

    return this.parseClaims(token).getExpiration();
  }

  private String getUserRole(String token) {
    String userRole = this.parseClaims(token).get("userRole", String.class);
    return userRole;
  }

  private String getUserId(String token) {
    Long userId = this.parseClaims(token).get("userId", Long.class);
    return userId.toString();
  }

  private String getUsername(String token) {
    return this.parseClaims(token).get("userId", String.class);
  }

  private Claims parseClaims(String token) {
    try {
      return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    } catch (SecurityException | MalformedJwtException | SignatureException e) {
      throw CustomException.from(GatewayErrorCode.INVALID_JWT_SIGNATURE);
    } catch (ExpiredJwtException e) {
      throw CustomException.from(GatewayErrorCode.EXPIRED_JWT_TOKEN);
    } catch (UnsupportedJwtException e) {
      throw CustomException.from(GatewayErrorCode.UNSUPPORTED_JWT_TOKEN);
    } catch (IllegalArgumentException e) {
      throw CustomException.from(GatewayErrorCode.JWT_CLAIMS_EMPTY);
    }
  }

  @PostConstruct
  private void setKey() {
    key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.secretKey));
  }
}