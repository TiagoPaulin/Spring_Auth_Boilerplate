package com.boilerplate.springauth.models;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.boilerplate.springauth.models.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @NotBlank
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    @Email
    @NotBlank
    @Size(max = 255)
    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;
    @JsonIgnore
    @NotBlank
    @Column(name = "password", nullable = false)
    private String password;
    @Column(name = "active", nullable = false)
    private boolean active = true;
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.USER;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public User () {}

    public User (Long id, String name, String email, String password, boolean active, Role role, LocalDateTime createdAt, LocalDateTime updatedAt) {

        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.active = active;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;

    }

    public Long getId () { return id; }
    public String getName () { return name; }
    public String getEmail () { return email; }
    public String getPassword () { return password; }
    public boolean isActive () { return active; }
    public Role getRole () { return role; }
    public LocalDateTime getCreatedAt () { return createdAt; }
    public LocalDateTime getUpdatedAt () { return updatedAt; }

    public void setId (Long id) { this.id = id; }
    public void setName (String name) { this.name = name; }
    public void setEmail (String email) { this.email = email; }
    public void setPassword (String password) { this.password = password; }
    public void setActive (boolean active) { this.active = active; }
    public void setRole (Role role) { this.role = role; }
    public void setCreatedAt (LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt (LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        
        if (this.role == Role.ADMIN) return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"));
        else return List.of(new SimpleGrantedAuthority("ROLE_USER"));

    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }



}
