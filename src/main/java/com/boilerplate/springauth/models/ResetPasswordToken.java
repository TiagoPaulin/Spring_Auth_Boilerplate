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
@Table(name = "users_reset_password_tokens")
public class ResetPasswordToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "token", nullable = false, unique = true)
    private String token;
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonIgnore
    private User user;
    @Column(nullable = false, name = "expiry_date")
    private Instant expiryDate;
    @Column(name = "used", nullable = false)
    private boolean used = false;

    public ResetPasswordToken () {}

    public ResetPasswordToken (Long id, String token, User user, Instant expiryDate) {

        this.id = id;
        this.token = token;
        this.user = user;
        this.expiryDate = expiryDate;

    }

    public Long getId () { return id; }
    public String getToken () { return token; }
    public User getUser () { return user; }
    public Instant getExpiryDate () { return expiryDate; }
    public boolean getUsed () { return used; }

    public void setId (Long id) { this.id = id; }
    public void setToken (String token) { this.token = token; }
    public void setUser (User user) { this.user = user; }
    public void setExpiryDate (Instant expiryDate) { this.expiryDate = expiryDate; }
    public void setUsed (boolean used) { this.used = used; }
    
    public boolean isExpired () { return this.expiryDate.isBefore(Instant.now()) || this.used; }

    
}
