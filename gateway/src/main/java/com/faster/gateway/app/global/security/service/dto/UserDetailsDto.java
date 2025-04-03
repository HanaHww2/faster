package com.faster.gateway.app.global.security.service.dto;

import com.common.resolver.dto.UserRole;
import java.util.ArrayList;
import java.util.Collection;
import lombok.Builder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Builder
public record UserDetailsDto(
    Long id,

    String username,

    String password,

    UserRole role
) {

  public static UserDetailsDto from(Authenticated user) {
    return new UserDetailsDto(
        user.getId(),
        user.getUsername(),
        user.getPassword(),
        user.getRole()
    );
  }

  public static UserDetailsDto of(String userId, String userRole) {
    return UserDetailsDto.builder()
        .id(Long.valueOf(userId))
        .role(UserRole.valueOf(userRole))
        .build();
  }

  public Collection<? extends GrantedAuthority> getAuthorities() {
    Collection<GrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority(role.name()));
    return authorities;
  }
}
