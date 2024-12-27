package com.cloud_storage.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "users", indexes = {@Index(name = "idx_login", columnList = "login")})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true, nullable = false)
    private String login;


    @Column(nullable = false)
    private String password;

    public User(String login, String password) {
        this.login = login;
        this.password = password;
    }
}
