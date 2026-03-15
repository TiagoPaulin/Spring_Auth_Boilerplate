package com.boilerplate.springauth.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.boilerplate.springauth.models.RefreshToken;
import com.boilerplate.springauth.models.User;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    Optional<RefreshToken> findByToken (String token);

    void deleteByUser (User user);

}
