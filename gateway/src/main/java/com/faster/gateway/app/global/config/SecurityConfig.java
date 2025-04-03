package com.faster.gateway.app.global.config;

import com.faster.gateway.app.global.security.handler.ExceptionHandlingFilter;
import com.faster.gateway.app.global.security.jwt.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration
@EnableReactiveMethodSecurity
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final ExceptionHandlingFilter exceptionHandlingFilter;
  private final JwtAuthenticationFilter authenticationFilter;
  private final ReactiveUserDetailsService userDetailsService;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
    http
        .formLogin(form -> form.disable())
        .logout(logout -> logout.disable())
        .httpBasic(basicSpec -> basicSpec.disable())
        .csrf(csrf -> csrf.disable())
        .securityContextRepository(NoOpServerSecurityContextRepository.getInstance()) // 세션을 비활성화
        .authorizeExchange(authorize -> authorize
            .pathMatchers("/api/auth/**",
                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
            .anyExchange().authenticated()
        )
        .addFilterAt(this.authenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
        .addFilterAt(this.exceptionHandlingFilter, SecurityWebFiltersOrder.LAST);

    return http.build();
  }

  @Bean
  public ReactiveAuthenticationManager authenticationManager() {
    UserDetailsRepositoryReactiveAuthenticationManager authenticationManager =
        new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);
    authenticationManager.setPasswordEncoder(passwordEncoder());
    return authenticationManager;
  }
}
