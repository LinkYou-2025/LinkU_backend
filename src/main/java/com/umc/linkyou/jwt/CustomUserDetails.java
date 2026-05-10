package com.umc.linkyou.jwt;

import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.apiPayload.code.status.auth.AuthErrorStatus;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final String password;
    private final Long userId;
    private final Role role;
    private final String provider;

    // 엔티티 기반 (OAuth2 핸들러 등에서 Users 객체가 있을 때)
    public CustomUserDetails(Users users, String provider) {
        this.password = users.getPassword() != null ? users.getPassword() : "";
        this.userId   = users.getId();
        this.role     = users.getRole();
        this.provider = provider;
    }

    // 클레임 기반 (필터에서 DB 없이 토큰 클레임으로 구성)
    public CustomUserDetails(Long userId, Role role, String provider) {
        if (role == null) {
            throw new UserHandler(AuthErrorStatus.UNAUTHORIZED);
        }
        this.password = "";
        this.userId   = userId;
        this.role     = role;
        this.provider = provider;
    }

    public Long getUserId() {
        return userId;
    }

    public Role getRole() {
        return role;
    }

    public String getProvider() {
        return provider;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.getAuthority()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return String.valueOf(userId);
    }
}
