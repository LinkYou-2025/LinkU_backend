package com.umc.linkyou.config.security.jwt;

import com.umc.linkyou.domain.Users;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

public class CustomUserDetails implements UserDetails {

    private final Users users; //User객체 자체는 변경 불가해야 함. 내부 name같은 건 ok
    private String provider;

    public CustomUserDetails(Users users){
        this.users = users;
    }

    public CustomUserDetails(Users users, String provider) {
        this.users = users;
        this.provider = provider;
    }
    public String getProvider() { return provider; }

    //권한 반환 로직:
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collection = new ArrayList<>();
        collection.add(new SimpleGrantedAuthority(users.getRole().getAuthority())); //ROLE_ 로 시작하는 방식으로 변경
        return collection;
    }


    @Override
    public String getPassword() {
        return users.getPassword();
    }

    @Override
    public String getUsername() {
        return users.getNickName();
    }
    public Users getUsers() {
        return users;
    }
}

