package com.boilerplate.springauth.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.boilerplate.springauth.models.ResetPasswordToken;
import com.boilerplate.springauth.models.User;

@Repository
public interface ResetPasswordTokenRepository extends JpaRepository<ResetPasswordToken, Long> {

    Optional<ResetPasswordToken> findByToken (String token);

    void deleteByUser (User user);

}
