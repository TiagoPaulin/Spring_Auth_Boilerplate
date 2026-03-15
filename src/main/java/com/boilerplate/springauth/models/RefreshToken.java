package com.boilerplate.springauth.models;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "users_refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "token", nullable = false, unique = true)
    private String token;
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonIgnore
    private User user;
    @Column(nullable = false)
    private Instant expiryDate;

    public RefreshToken () {}

    public RefreshToken (Long id, String token, User user, Instant expiryDate) {

        this.id = id;
        this.token = token;
        this.user = user;
        this.expiryDate = expiryDate;

    }

    public Long getId () { return id; }
    public String getToken () { return token; }
    public User getUser () { return user; }
    public Instant getExpiryDate () { return expiryDate; }

    public void setId (Long id) { this.id = id; }
    public void setToken (String token) { this.token = token; }
    public void setUser (User user) { this.user = user; }
    public void setExpiryDate (Instant expiryDate) { this.expiryDate = expiryDate; }
    
    public boolean isExpired () { return this.expiryDate.isBefore(Instant.now()); }

}
