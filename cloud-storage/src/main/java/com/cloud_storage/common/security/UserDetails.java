package com.cloud_storage.security;

import com.cloud_storage.entity.User;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

public class UserDetails implements org.springframework.security.core.userdetails.UserDetails {
    private final User person;

    public UserDetails(User person) {
        this.person = person;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return this.person.getPassword();
    }

    @Override
    public String getUsername() {
        return this.person.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
//        return org.springframework.security.core.userdetails.UserDetails.super.isAccountNonExpired();
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
//        return org.springframework.security.core.userdetails.UserDetails.super.isAccountNonLocked();
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
//        return org.springframework.security.core.userdetails.UserDetails.super.isCredentialsNonExpired();
        return true;
    }

    @Override
    public boolean isEnabled() {
//        return org.springframework.security.core.userdetails.UserDetails.super.isEnabled();
        return true;
    }
    public User getPerson() {
        return this.person;
    }
}