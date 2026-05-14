package com.umc.linkyou.config.security.jwt;

import com.umc.linkyou.domain.Users;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

public class CustomUserDetails implements UserDetails {

    private Users users;
    private String provider;

    public CustomUserDetails(Users users){
        this.users = users;
    }

    public CustomUserDetails(Users users, String provider) {
        this.users = users;
        this.provider = provider;
    }
    public String getProvider() { return provider; }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collection = new ArrayList<>();
        collection.add(new SimpleGrantedAuthority(users.getRole().name()));
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

